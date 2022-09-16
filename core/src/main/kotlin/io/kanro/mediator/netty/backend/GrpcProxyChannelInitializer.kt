package io.kanro.mediator.netty.backend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInitializer
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2FrameLogger
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.ssl.ApplicationProtocolConfig
import io.netty.handler.ssl.ApplicationProtocolNames
import io.netty.handler.ssl.SslContextBuilder

class GrpcProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(LoggingHandler(LogLevel.INFO)).addLast(
            Http2FrameCodecBuilder.forClient()
                .frameLogger(Http2FrameLogger(LogLevel.INFO, GrpcStreamBackendHandler::class.java))
                .initialSettings(
                    Http2Settings()
                        .pushEnabled(false)
                        .maxConcurrentStreams(0)
                        .initialWindowSize(1048576)
                        .maxHeaderListSize(8192)
                ).build()
        ).addLast(Http2MultiplexHandler(GrpcStreamBackendInboundHandler()))
    }
}

class GrpcSslProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        val context = SslContextBuilder.forClient()
            .applicationProtocolConfig(
                ApplicationProtocolConfig(
                    ApplicationProtocolConfig.Protocol.ALPN,
                    ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                    ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                    ApplicationProtocolNames.HTTP_2
                )
            )
            .build()

        ch.pipeline()
            .addLast(context.newHandler(ch.alloc()))
            .addLast(LoggingHandler(LogLevel.INFO))
            .addLast(
                Http2FrameCodecBuilder.forClient()
                    .frameLogger(Http2FrameLogger(LogLevel.INFO, GrpcStreamBackendHandler::class.java))
                    .initialSettings(
                        Http2Settings()
                            .pushEnabled(false)
                            .maxConcurrentStreams(0)
                            .initialWindowSize(1048576)
                            .maxHeaderListSize(8192)
                    )
                    .build()
            ).addLast(Http2MultiplexHandler(GrpcStreamBackendInboundHandler()))
    }
}

class GrpcProxyTransparentChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast("logger", LoggingHandler(LogLevel.INFO))
            .addLast()
    }
}

class TransparentProxyHandler : SimpleChannelInboundHandler<ByteBuf>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val frontend = ctx.channel().attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY).get()
        frontend.writeAndFlush(msg.retain())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        ctx.channel().attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY).get().close()
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ctx.channel().attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY).get().close()
        super.channelInactive(ctx)
    }
}
