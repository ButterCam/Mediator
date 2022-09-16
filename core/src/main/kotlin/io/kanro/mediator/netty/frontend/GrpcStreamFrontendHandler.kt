package io.kanro.mediator.netty.frontend

import com.bybutter.sisyphus.rpc.Code
import io.kanro.mediator.netty.GrpcProxySupport
import io.kanro.mediator.netty.backend.GrpcStreamBackendHandler
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
import io.netty.handler.codec.http2.Http2StreamChannelBootstrap
import io.netty.handler.codec.http2.Http2StreamFrame

@ChannelHandler.Sharable
class GrpcStreamFrontendHandler : SimpleChannelInboundHandler<Http2StreamFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Http2StreamFrame) {
        val backend = ctx.channel().attr(GrpcProxySupport.BACKEND_STREAM_CHANNEL_KEY).get()
        val support = ctx.channel().parent().attr(GrpcProxySupport.KEY).get()
        when (msg) {
            is Http2DataFrame -> {
                backend.writeAndFlush(support.onRequest(ctx.channel() as Http2StreamChannel, backend, msg))
            }

            is Http2HeadersFrame -> {
                backend.writeAndFlush(support.onRequestHeader(ctx.channel() as Http2StreamChannel, backend, msg))
            }
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any?) {
        val backend = ctx.channel().attr(GrpcProxySupport.BACKEND_STREAM_CHANNEL_KEY).get()
        val support = ctx.channel().parent().attr(GrpcProxySupport.KEY).get()
        when (evt) {
            is Http2ResetFrame -> {
                backend.writeAndFlush(DefaultHttp2ResetFrame(evt.errorCode()))
                support.onHttp2Error(
                    ctx.channel() as Http2StreamChannel,
                    backend,
                    ctx.channel() as Http2StreamChannel,
                    evt.errorCode().toInt()
                )
            }

            is Http2GoAwayFrame -> {
                backend.parent()
                    .writeAndFlush(DefaultHttp2GoAwayFrame(evt.errorCode(), evt.content().retainedDuplicate()))
                backend.parent().closeFuture()
                ctx.channel().parent().closeFuture()
                support.onHttp2Error(
                    ctx.channel() as Http2StreamChannel,
                    backend,
                    ctx.channel() as Http2StreamChannel,
                    evt.errorCode().toInt()
                )
            }
        }
        super.userEventTriggered(ctx, evt)
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        val backend = ctx.channel().parent().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).get()
        val backendStream = Http2StreamChannelBootstrap(backend).attr(
            GrpcProxySupport.FRONTEND_STREAM_CHANNEL_KEY, ctx.channel() as Http2StreamChannel
        ).handler(GrpcStreamBackendHandler()).open().await().get()
        ctx.channel().attr(GrpcProxySupport.BACKEND_STREAM_CHANNEL_KEY).set(backendStream)
        super.handlerAdded(ctx)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable?) {
        val backend = ctx.channel().attr(GrpcProxySupport.BACKEND_STREAM_CHANNEL_KEY).get()
        val backendStream = ctx.channel().attr(GrpcProxySupport.BACKEND_STREAM_CHANNEL_KEY).get()

        ctx.close()
        ctx.channel().parent().close()

        backendStream?.close()
        backend.close()

        val support = ctx.channel().parent().attr(GrpcProxySupport.KEY).get()
        support.onHttp2Error(
            ctx.channel() as Http2StreamChannel, backend, ctx.channel() as Http2StreamChannel, Code.INTERNAL.number
        )
    }
}
