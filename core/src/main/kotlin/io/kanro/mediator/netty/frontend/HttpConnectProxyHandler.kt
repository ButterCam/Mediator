package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus

class HttpConnectProxyHandler : SimpleChannelInboundHandler<HttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        when (msg.method()) {
            HttpMethod.CONNECT -> {
                val host = msg.headers().get("Host")
                if (host == null) {
                    ctx.writeAndFlush(DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.BAD_REQUEST))
                }
                ctx.channel().attr(GrpcProxySupport.TARGET_HOST_KEY).set(host)

                ctx.writeAndFlush(DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK))
                ctx.pipeline().remove("httpHandler")
                ctx.pipeline().remove("httpAggregator")
                ctx.pipeline().remove("httpCodec")
                ctx.pipeline().addLast(GrpcOptionalSslHandler())
            }

            HttpMethod.GET -> {
                ctx.writeAndFlush(EchoService.buildEchoResponse(ctx, msg))
                ctx.close()
            }

            HttpMethod.POST -> {
                ctx.writeAndFlush(EchoService.buildEchoResponse(ctx, msg))
                ctx.close()
            }

            else -> {
                ctx.writeAndFlush(DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.METHOD_NOT_ALLOWED))
                ctx.close()
            }
        }
    }
}
