package io.kanro.mediator.proxy.backend.tcp

import io.kanro.mediator.proxy.ChannelInitializer
import io.kanro.mediator.proxy.backend.BackendChannelProvider
import io.kanro.mediator.proxy.backend.ProxyProtocolProvider
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel

class TcpProxyProvider : ProxyProtocolProvider, ChannelInitializer<SocketChannel> {
    override val protocol: String = BackendChannelProvider.TCP

    override fun init(ch: SocketChannel) {
        ch.pipeline().apply {
            addLast(TcpProxyHandler())
        }
    }
}

class TcpProxyHandler : SimpleChannelInboundHandler<ByteBuf>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val frontend = ctx.channel().attr(BackendChannelProvider.frontendChannelKey).get()
        frontend.writeAndFlush(msg.retain())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        println("Exception: ${cause?.message}")
        ctx.channel().attr(BackendChannelProvider.frontendChannelKey).get().close()
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ctx.channel().attr(BackendChannelProvider.frontendChannelKey).get().close()
        super.channelInactive(ctx)
    }
}
