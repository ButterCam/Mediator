package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.kanro.mediator.netty.backend.GrpcProxyChannelInitializer
import io.kanro.mediator.netty.backend.GrpcProxyTransparentChannelInitializer
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2FrameLogger
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.SslHandler
import io.netty.util.ReferenceCountUtil

class GrpcOptionalSslHandler : ByteToMessageDecoder() {
    private class TransparentDecoder : ByteToMessageDecoder() {
        override fun decode(ctx: ChannelHandlerContext, input: ByteBuf, out: MutableList<Any>) {
            out.add(input.readBytes(input.readableBytes()))
        }
    }

    override fun decode(context: ChannelHandlerContext, input: ByteBuf, out: List<Any?>?) {
        if (input.readableBytes() < 5) {
            return
        }
        if (SslHandler.isEncrypted(input)) {
            handleSsl(context)
        } else {
            handleNonSsl(context)
        }
    }

    private fun handleSsl(context: ChannelHandlerContext) {
        context.channel().attr(GrpcProxySupport.FRONTEND_SSL_ENABLE_KEY).set(true)
        val support = context.channel().attr(GrpcProxySupport.KEY).get()
        val host = context.channel().attr(GrpcProxySupport.TARGET_HOST_KEY).get()
        val sslContext = support.sslContext(context.channel(), host)

        if (sslContext == null) {
            context.pipeline().replace(this, null, TransparentDecoder())
            handleTransparent(context)
            return
        }

        var sslHandler: SslHandler? = null
        try {
            sslHandler = sslContext.newHandler(context.alloc())
            context.pipeline().replace(this, null, sslHandler)
                .addLast(LoggingHandler(LogLevel.INFO))
            handleHttp2(context)
            sslHandler = null
        } finally {
            // Since the SslHandler was not inserted into the pipeline the ownership of the SSLEngine was not
            // transferred to the SslHandler.
            if (sslHandler != null) {
                ReferenceCountUtil.safeRelease(sslHandler.engine())
            }
        }
    }

    private fun handleNonSsl(context: ChannelHandlerContext) {
        context.channel().attr(GrpcProxySupport.FRONTEND_SSL_ENABLE_KEY).set(false)
        context.pipeline().replace(this, null, TransparentDecoder())
        handleHttp2(context)
    }

    private fun handleHttp2(context: ChannelHandlerContext) {
        context.pipeline().addLast(
            Http2FrameCodecBuilder.forServer()
                .frameLogger(Http2FrameLogger(LogLevel.INFO, GrpcStreamFrontendHandler::class.java))
                .build()
        )
        context.pipeline().addLast(Http2MultiplexHandler(GrpcStreamFrontendHandler()))
        val support = context.channel().attr(GrpcProxySupport.KEY).get()
        val host = context.channel().attr(GrpcProxySupport.TARGET_HOST_KEY).get()
        val backend = support.connectToBackend(
            context.channel(), backendBootstrap(context.channel()), host
        ).await().channel()
        context.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).set(backend)
    }

    private fun handleTransparent(context: ChannelHandlerContext) {
        context.pipeline().addLast(TransparentProxyHandler())
        val support = context.channel().attr(GrpcProxySupport.KEY).get()
        val host = context.channel().attr(GrpcProxySupport.TARGET_HOST_KEY).get()
        val backend = support.connectToBackendTransparent(
            context.channel(), transparentBootstrap(context.channel()), host
        ).await().channel()
        context.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).set(backend)
    }

    private fun backendBootstrap(frontend: Channel): Bootstrap {
        val support = frontend.attr(GrpcProxySupport.KEY).get()
        val workgroup = frontend.attr(GrpcProxyServer.WORK_GROUP_KEY).get()
        return Bootstrap().channel(NioSocketChannel::class.java).group(workgroup).attr(GrpcProxySupport.KEY, support)
            .attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY, frontend)
            .handler(GrpcProxyChannelInitializer())
            .option(ChannelOption.SO_KEEPALIVE, true)
    }

    private fun transparentBootstrap(frontend: Channel): Bootstrap {
        val support = frontend.attr(GrpcProxySupport.KEY).get()
        val workgroup = frontend.attr(GrpcProxyServer.WORK_GROUP_KEY).get()
        return Bootstrap().channel(NioSocketChannel::class.java).group(workgroup).attr(GrpcProxySupport.KEY, support)
            .attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY, frontend).handler(GrpcProxyTransparentChannelInitializer())
            .option(ChannelOption.SO_KEEPALIVE, true)
    }
}
