package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.kanro.mediator.netty.backend.GrpcProxyChannelInitializer
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelOption
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http2.Http2FrameCodecBuilder
import io.netty.handler.codec.http2.Http2FrameLogger
import io.netty.handler.codec.http2.Http2MultiplexHandler
import io.netty.handler.logging.LogLevel

class HttpConnectProxyHandler : SimpleChannelInboundHandler<HttpRequest>() {
    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        if (msg.method() == HttpMethod.CONNECT) {
            ctx.writeAndFlush(DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.OK))
            ctx.pipeline().remove("httpHandler")
            ctx.pipeline().remove("httpAggregator")
            ctx.pipeline().remove("httpCodec")
            ctx.pipeline().addLast(
                Http2FrameCodecBuilder.forServer()
                    .frameLogger(Http2FrameLogger(LogLevel.INFO, GrpcStreamFrontendHandler::class.java)).build()
            )
            ctx.pipeline().addLast(Http2MultiplexHandler(GrpcStreamFrontendHandler()))

            val support = ctx.channel().attr(GrpcProxySupport.KEY).get()
            val backend = support.connectToBackend(
                ctx.channel(), backendBootstrap(ctx.channel()), msg.headers().get("Host")
            ).await().channel()
            ctx.channel().attr(GrpcProxySupport.BACKEND_CHANNEL_KEY).set(backend)
        } else {
            ctx.writeAndFlush(DefaultFullHttpResponse(msg.protocolVersion(), HttpResponseStatus.METHOD_NOT_ALLOWED))
        }
    }

    private fun backendBootstrap(frontend: Channel): Bootstrap {
        val support = frontend.attr(GrpcProxySupport.KEY).get()
        val workgroup = frontend.attr(GrpcProxyServer.WORK_GROUP_KEY).get()
        return Bootstrap().channel(NioSocketChannel::class.java).group(workgroup)
            .attr(GrpcProxySupport.KEY, support)
            .attr(GrpcProxySupport.FRONTEND_CHANNEL_KEY, frontend)
            .handler(GrpcProxyChannelInitializer())
            .option(ChannelOption.SO_KEEPALIVE, true)
    }
}
