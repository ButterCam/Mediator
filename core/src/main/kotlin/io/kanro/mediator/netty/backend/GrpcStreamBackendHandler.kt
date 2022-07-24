package io.kanro.mediator.netty.backend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2StreamChannel
import io.netty.handler.codec.http2.Http2StreamFrame

@ChannelHandler.Sharable
class GrpcStreamBackendHandler : SimpleChannelInboundHandler<Http2StreamFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Http2StreamFrame) {
        val frontend = ctx.channel().attr(GrpcProxySupport.FRONTEND_STREAM_CHANNEL_KEY).get()
        val support = ctx.channel().parent().attr(GrpcProxySupport.KEY).get()
        when (msg) {
            is Http2DataFrame -> {
                frontend.writeAndFlush(support.onResponse(frontend, ctx.channel() as Http2StreamChannel, msg))
            }
            is Http2HeadersFrame -> {
                if (msg.isEndStream) {
                    frontend.writeAndFlush(support.onTrailer(frontend, ctx.channel() as Http2StreamChannel, msg))
                } else {
                    frontend.writeAndFlush(support.onResponseHeader(frontend, ctx.channel() as Http2StreamChannel, msg))
                }
            }
        }
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
    }
}
