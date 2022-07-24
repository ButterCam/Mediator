package io.kanro.mediator.netty.backend

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2FrameLogger
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.codec.http2.Http2Settings
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

class GrpcProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline().addLast(LoggingHandler(LogLevel.INFO)).addLast(
            Http2FrameCodecBuilder.forClient()
                .frameLogger(Http2FrameLogger(LogLevel.INFO, GrpcStreamBackendHandler::class.java)).initialSettings(
                    Http2Settings().pushEnabled(false).maxConcurrentStreams(0).initialWindowSize(1048576)
                        .maxHeaderListSize(8192)
                ).build()
        ).addLast(Http2MultiplexHandler(GrpcStreamBackendInboundHandler()))
    }
}
