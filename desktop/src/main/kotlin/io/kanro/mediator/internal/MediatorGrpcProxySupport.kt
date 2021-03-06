package io.kanro.mediator.internal

import com.bybutter.sisyphus.security.base64Decode
import io.grpc.Codec
import io.grpc.Status
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.MediatorConfiguration
import io.kanro.mediator.desktop.model.MethodType
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.netty.GrpcCallDefinitions
import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2Headers
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.util.AttributeKey
import java.io.ByteArrayInputStream
import kotlin.math.min

class MediatorGrpcProxySupport(private val config: MediatorConfiguration) : GrpcProxySupport {
    override fun connectToBackend(frontend: Channel, backendBootstrap: Bootstrap, target: String): ChannelFuture {
        val rules = config.serverRules.filter {
            it.authority.matches(target)
        }
        val rewriteAuthority = rules.fold(target) { r, it ->
            if (it.enabled && it.replaceEnabled && it.replace.isNotBlank()) {
                it.authority.replace(target, it.replace)
            } else r
        }

        val (host, port) = rewriteAuthority.split(":")

        frontend.attr(REWRITE_AUTHORITY_KEY).set(rewriteAuthority)

        return backendBootstrap.connect(host, port.toInt())
    }

    override fun onRequestHeader(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        val timeline = CallTimeline()
        fs.attr(TIMELINE_KEY).set(timeline)
        val headers = frame.headers()
        fs.attr(REQUEST_READER_KEY).set(GrpcMessageReader(headers[GrpcCallDefinitions.GRPC_CODING_HEADER]?.toString()))
        timeline.start(
            headers.authority().toString(),
            fs.parent().attr(REWRITE_AUTHORITY_KEY).get(),
            headers.path().substring(1),
            MethodType.UNKNOWN,
            headers.toGrpcMetadata()
        )
        MainViewModel.calls += timeline
        val filter = MainViewModel.filter.value
        if (filter.isEmpty() || timeline.start().authority.contains(filter) || timeline.start().method.contains(filter)
        ) {
            MainViewModel.shownCalls += timeline
        }
        return super.onRequestHeader(fs, bs, frame)
    }

    override fun onRequest(fs: Http2StreamChannel, bs: Http2StreamChannel, frame: Http2DataFrame): Http2DataFrame {
        val buf = frame.content().duplicate()
        fs.attr(REQUEST_READER_KEY).get().read(buf).forEach {
            val timeline = fs.attr(TIMELINE_KEY).get()
            timeline.input(it)
        }
        return super.onRequest(fs, bs, frame)
    }

    override fun onResponseHeader(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        val headers = frame.headers()
        fs.attr(RESPONSE_READER_KEY).set(GrpcMessageReader(headers[GrpcCallDefinitions.GRPC_CODING_HEADER]?.toString()))
        val timeline = fs.attr(TIMELINE_KEY).get()
        timeline.accept(headers.toGrpcMetadata())
        return super.onResponseHeader(fs, bs, frame)
    }

    override fun onResponse(fs: Http2StreamChannel, bs: Http2StreamChannel, frame: Http2DataFrame): Http2DataFrame {
        val buf = frame.content().duplicate()
        fs.attr(RESPONSE_READER_KEY).get().read(buf).forEach {
            val timeline = fs.attr(TIMELINE_KEY).get()
            timeline.output(it)
        }
        return super.onResponse(fs, bs, frame)
    }

    override fun onTrailer(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        val headers = frame.headers()
        val timeline = fs.attr(TIMELINE_KEY).get()
        val status = headers.get("grpc-status")?.toString()?.toInt()?.let {
            Status.fromCodeValue(it)
        }
        timeline.close(status ?: Status.UNKNOWN, headers.toGrpcMetadata())
        return super.onResponseHeader(fs, bs, frame)
    }

    companion object {
        val REWRITE_AUTHORITY_KEY =
            AttributeKey.valueOf<String>(MediatorGrpcProxySupport::class.java, "rewriteAuthority")
        val TIMELINE_KEY =
            AttributeKey.valueOf<CallTimeline>(MediatorGrpcProxySupport::class.java, "timeline")
        val REQUEST_READER_KEY =
            AttributeKey.valueOf<GrpcMessageReader>(MediatorGrpcProxySupport::class.java, "request-reader")
        val RESPONSE_READER_KEY =
            AttributeKey.valueOf<GrpcMessageReader>(MediatorGrpcProxySupport::class.java, "response-reader")

        private fun Http2Headers.toGrpcMetadata(): io.grpc.Metadata {
            val result = io.grpc.Metadata()
            this.forEach { (k, v) ->
                if (k.endsWith("-bin")) {
                    result.put(k.binraryKey() ?: return@forEach, v.toString().base64Decode())
                } else {
                    result.put(k.stringKey() ?: return@forEach, v.toString())
                }
            }
            return result
        }

        private fun CharSequence.stringKey(): io.grpc.Metadata.Key<String>? {
            if (startsWith(':')) return null
            if (endsWith("-bin")) return null

            return io.grpc.Metadata.Key.of(this.toString(), io.grpc.Metadata.ASCII_STRING_MARSHALLER)
        }

        private fun CharSequence.binraryKey(): io.grpc.Metadata.Key<ByteArray>? {
            if (startsWith(':')) return null
            if (!endsWith("-bin")) return null

            return io.grpc.Metadata.Key.of(this.toString(), io.grpc.Metadata.BINARY_BYTE_MARSHALLER)
        }
    }
}

class GrpcMessageReader(compressMode: String?) {
    private var buffer: ByteArray? = null
    private var compressed = false
    private var pos: Int = 0
    private var buffers = mutableListOf<ByteArray>()
    private val codec = when (compressMode) {
        "gzip" -> Codec.Gzip()
        null, "identity", "none" -> Codec.Identity.NONE
        else -> throw IllegalArgumentException("Unsupported mode: $compressMode")
    }

    fun read(buf: ByteBuf): List<ByteArray> {
        read0(buf)
        if (buffers.isEmpty()) return listOf()
        return buffers.also {
            buffers = mutableListOf()
        }
    }

    private fun read0(buf: ByteBuf) {
        if (buf.readableBytes() == 0) return

        var bytes = buffer
        if (bytes == null) {
            pos = 0
            compressed = buf.readByte() > 0.toByte()
            bytes = ByteArray(buf.readUnsignedInt().toInt())
            this.buffer = bytes
        }

        val currentIndex = buf.readerIndex()
        buf.readBytes(bytes, pos, min(bytes.size - pos, buf.readableBytes()))
        pos += buf.readerIndex() - currentIndex

        if (bytes.size == pos) {
            buffers += if (compressed) {
                codec.decompress(ByteArrayInputStream(bytes)).readAllBytes()
            } else {
                bytes
            }
            this.buffer = null
        }

        return read0(buf)
    }
}
