package io.kanro.mediator.proxy.frontend.http

import io.kanro.mediator.proxy.ChannelInitializer
import io.kanro.mediator.proxy.frontend.ProxyProtocolProvider
import io.kanro.mediator.proxy.frontend.ProxyServer
import io.kanro.mediator.proxy.frontend.tcp.TcpProxyHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpServerCodec

class HttpProxyProvider : ProxyProtocolProvider, ChannelInitializer<SocketChannel> {
    override val protocol: String = ProxyServer.HTTP

    override fun init(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast("httpCodec", HttpServerCodec())
            addLast("httpAggregator", HttpObjectAggregator(1048576))
            addLast("httpHandler", HttpFrontendProxy())
        }
    }
}

class HttpFrontendProxy : SimpleChannelInboundHandler<HttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        if (msg.method() == HttpMethod.CONNECT) {
            val response = DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK)
            ctx.writeAndFlush(response)
            ctx.pipeline().remove("httpHandler")
            ctx.pipeline().remove("httpAggregator")
            ctx.pipeline().remove("httpCodec")
            ctx.pipeline().addLast(TcpProxyHandler())
        }
    }
}
