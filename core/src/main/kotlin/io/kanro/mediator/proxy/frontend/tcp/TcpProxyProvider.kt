package io.kanro.mediator.proxy.frontend.tcp

import io.kanro.mediator.proxy.ChannelInitializer
import io.kanro.mediator.proxy.frontend.ProxyProtocolProvider
import io.kanro.mediator.proxy.frontend.ProxyServer
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel

class TcpProxyProvider : ProxyProtocolProvider, ChannelInitializer<SocketChannel> {
    override val protocol: String = ProxyServer.TCP

    override fun init(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast(TcpProxyHandler())
        }
    }
}

class TcpProxyHandler : SimpleChannelInboundHandler<ByteBuf>() {
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val backendProvider = ctx.channel().attr(ProxyServer.backendProviderKey).get()
        ctx.channel().attr(ProxyServer.backendChannelKey).set(backendProvider.channel(ctx.channel()))
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val backend = ctx.channel().attr(ProxyServer.backendChannelKey).get().channel()
        backend.writeAndFlush(msg.retain())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        println("Exception: ${cause?.message}")
        ctx.channel().attr(ProxyServer.backendChannelKey).get().channel().close()
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ctx.channel().attr(ProxyServer.backendChannelKey).get().channel().close()
        super.channelInactive(ctx)
    }
}
