package io.kanro.mediator.netty.frontend

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler

class GrpcProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline()
            .addLast(LoggingHandler(LogLevel.INFO))
            .addLast("httpCodec", HttpServerCodec())
            .addLast("httpAggregator", HttpObjectAggregator(1048576))
            .addLast("httpHandler", HttpConnectProxyHandler())
    }
}
