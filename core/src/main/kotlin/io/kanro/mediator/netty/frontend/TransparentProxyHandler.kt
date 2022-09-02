package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler

class TransparentProxyHandler : SimpleChannelInboundHandler<ByteBuf>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: ByteBuf) {
        val backend = ctx.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).get()
        backend.writeAndFlush(msg.retain())
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        ctx.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).get().close()
        ctx.close()
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        ctx.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).get().close()
        super.channelInactive(ctx)
    }
}
