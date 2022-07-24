package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.util.AttributeKey

class GrpcProxyServer(private val port: Int, private val interceptor: GrpcProxySupport) {
    private val bossGroup: EventLoopGroup = NioEventLoopGroup()
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()

    private val bootstrap = ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel::class.java)
        .handler(LoggingHandler(LogLevel.INFO))
        .attr(GrpcProxySupport.KEY, interceptor)
        .attr(WORK_GROUP_KEY, workerGroup)
        .childAttr(GrpcProxySupport.KEY, interceptor)
        .childAttr(WORK_GROUP_KEY, workerGroup)
        .childHandler(GrpcProxyChannelInitializer())
        .childOption(ChannelOption.SO_KEEPALIVE, true)
    private lateinit var channel: ChannelFuture

    fun run(): GrpcProxyServer {
        channel = bootstrap.bind(port)
        return this
    }

    fun close() {
        if (::channel.isInitialized) {
            try {
                channel.channel().close().sync()
            } finally {
            }
        }
        bossGroup.shutdownGracefully()
        workerGroup.shutdownGracefully()
    }

    companion object {
        val WORK_GROUP_KEY = AttributeKey.valueOf<EventLoopGroup>(GrpcProxySupport::class.java, "proxy-work-group")
    }
}
