package io.kanro.mediator.netty

import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.ssl.SslContext
import io.netty.util.AttributeKey
import java.security.cert.X509Certificate

fun Http2HeadersFrame.duplicate(): Http2HeadersFrame {
    return DefaultHttp2HeadersFrame(this.headers(), this.isEndStream)
}

interface GrpcProxySupport {
    fun connectToBackend(frontend: Channel, backendBootstrap: Bootstrap, target: String): ChannelFuture

    fun connectToBackendTransparent(frontend: Channel, backendBootstrap: Bootstrap, target: String): ChannelFuture

    fun sslContext(frontend: Channel, target: String): SslContext?

    fun getCertificateAuthority(): X509Certificate

    fun onRequestHeader(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        return frame.duplicate()
    }

    fun onRequest(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2DataFrame
    ): Http2DataFrame {
        return frame.retainedDuplicate()
    }

    fun onResponseHeader(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        return frame.duplicate()
    }

    fun onResponse(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2DataFrame
    ): Http2DataFrame {
        return frame.retainedDuplicate()
    }

    fun onTrailer(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        frame: Http2HeadersFrame
    ): Http2HeadersFrame {
        return frame.duplicate()
    }

    fun onHttp2Error(
        fs: Http2StreamChannel,
        bs: Http2StreamChannel,
        causeChannel: Http2StreamChannel,
        errorCode: Int
    ) {
    }

    companion object {
        val KEY = AttributeKey.valueOf<GrpcProxySupport>(GrpcProxySupport::class.java, "grpc-proxy-support")

        val TARGET_HOST_KEY = AttributeKey.valueOf<String>(String::class.java, "grpc-host")

        val FRONTEND_SSL_ENABLE_KEY = AttributeKey.valueOf<Boolean>(Boolean::class.java, "grpc-frontend-ssl")

        val BACKEND_SSL_ENABLE_KEY = AttributeKey.valueOf<Boolean>(Boolean::class.java, "grpc-backend-ssl")

        val FRONTEND_CHANNEL_KEY = AttributeKey.valueOf<Channel>(GrpcProxySupport::class.java, "grpc-frontend")

        val BACKEND_CHANNEL_KEY = AttributeKey.valueOf<Channel>(GrpcProxySupport::class.java, "grpc-backend")

        val FRONTEND_STREAM_CHANNEL_KEY =
            AttributeKey.valueOf<Http2StreamChannel>(GrpcProxySupport::class.java, "grpc-frontend-stream")

        val BACKEND_STREAM_CHANNEL_KEY =
            AttributeKey.valueOf<Http2StreamChannel>(GrpcProxySupport::class.java, "grpc-backend-stream")
    }
}
