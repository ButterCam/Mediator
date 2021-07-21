package io.kanro.mediator.proxy.frontend

import com.bybutter.sisyphus.spi.ServiceLoader
import io.kanro.mediator.proxy.backend.BackendChannelProvider
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.AttributeKey

class ProxyServer(private val protocol: String, private val port: Int) {
    private val bossGroup: EventLoopGroup = NioEventLoopGroup()
    private val workerGroup: EventLoopGroup = NioEventLoopGroup()
    private val proxyProvider = ServiceLoader.load(ProxyProtocolProvider::class.java).firstOrNull {
        it.protocol == protocol
    } ?: throw UnsupportedOperationException("Unsupported proxy protocol '$protocol'")
    private val bootstrap = ServerBootstrap()
        .group(bossGroup, workerGroup)
        .channel(NioServerSocketChannel::class.java)
        .childHandler(object : ChannelInitializer<SocketChannel>() {
            override fun initChannel(ch: SocketChannel) {
                if (proxyProvider is io.kanro.mediator.proxy.ChannelInitializer<*>) {
                    (proxyProvider as io.kanro.mediator.proxy.ChannelInitializer<SocketChannel>).init(ch)
                }
            }
        })
        .apply {
            if (proxyProvider is BootstrapHandler) {
                proxyProvider.handle(this)
            }
        }
        .childOption(ChannelOption.SO_KEEPALIVE, true)
    private lateinit var channel: ChannelFuture

    fun run(backendProvider: BackendChannelProvider): ProxyServer {
        channel = bootstrap.childAttr(backendProviderKey, backendProvider)
            .bind(port)
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
        const val HTTP = "http"
        const val TCP = "tcp"

        val backendProviderKey: AttributeKey<BackendChannelProvider> =
            AttributeKey.valueOf(BackendChannelProvider::class.java, "provider")

        val backendChannelKey: AttributeKey<ChannelFuture> =
            AttributeKey.valueOf(BackendChannelProvider::class.java, "backendChannel")
    }
}
