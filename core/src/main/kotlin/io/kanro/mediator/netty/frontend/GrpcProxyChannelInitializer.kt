package io.kanro.mediator.netty.frontend

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec

class GrpcProxyChannelInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(ch: SocketChannel) {
        ch.pipeline()
            .addLast("httpCodec", HttpServerCodec())
            .addLast("httpAggregator", HttpObjectAggregator(1048576))
            .addLast("httpHandler", HttpConnectProxyHandler())
    }
}
