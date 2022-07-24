package io.kanro.mediator.netty.backend

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http2.DefaultHttp2ResetFrame
import io.netty.handler.codec.http2.Http2Error
import io.netty.handler.codec.http2.Http2StreamFrame

class GrpcStreamBackendInboundHandler : SimpleChannelInboundHandler<Http2StreamFrame>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Http2StreamFrame) {
        ctx.writeAndFlush(
            DefaultHttp2ResetFrame(
                Http2Error.REFUSED_STREAM
            )
        )
    }
}
