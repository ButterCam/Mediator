package io.kanro.mediator.netty.backend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http2.DefaultHttp2GoAwayFrame
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame
import io.netty.handler.codec.http2.Http2DataFrame
import io.netty.handler.codec.http2.Http2GoAwayFrame
import io.netty.handler.codec.http2.Http2HeadersFrame
import io.netty.handler.codec.http2.Http2ResetFrame
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

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        val frontend = ctx.channel().attr(GrpcProxySupport.FRONTEND_STREAM_CHANNEL_KEY).get()
        val support = ctx.channel().parent().attr(GrpcProxySupport.KEY).get()
        when (evt) {
            is Http2ResetFrame -> {
                frontend.writeAndFlush(DefaultHttp2ResetFrame(evt.errorCode()))
                support.onHttp2Error(
                    frontend,
                    ctx.channel() as Http2StreamChannel,
                    ctx.channel() as Http2StreamChannel,
                    evt.errorCode().toInt()
                )
            }

            is Http2GoAwayFrame -> {
                frontend.parent()
                    .writeAndFlush(DefaultHttp2GoAwayFrame(evt.errorCode(), evt.content().retainedDuplicate()))
                frontend.parent().closeFuture()
                ctx.channel().parent().closeFuture()
                support.onHttp2Error(
                    frontend,
                    ctx.channel() as Http2StreamChannel,
                    ctx.channel() as Http2StreamChannel,
                    evt.errorCode().toInt()
                )
            }
        }
        super.userEventTriggered(ctx, evt)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        super.handlerAdded(ctx)
    }
}
