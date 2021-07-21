package io.kanro.mediator.proxy.backend

import com.bybutter.sisyphus.spi.ServiceLoader
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.AttributeKey

class BackendChannelProvider(private val protocol: String, private val host: String, private val port: Int) {
    private val eventLoop: EventLoopGroup = NioEventLoopGroup()

    fun channel(frontendChannel: Channel): ChannelFuture {
        val proxyProvider = ServiceLoader.load(ProxyProtocolProvider::class.java).firstOrNull {
            it.protocol == protocol
        } ?: throw UnsupportedOperationException("Unsupported proxy protocol '$protocol'")

        return Bootstrap()
            .group(eventLoop)
            .channel(NioSocketChannel::class.java)
            .attr(frontendChannelKey, frontendChannel)
            .handler(object : ChannelInitializer<SocketChannel>() {
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
            .connect(host, port)
    }

    companion object {
        const val TCP = "tcp"

        val frontendChannelKey: AttributeKey<Channel> =
            AttributeKey.valueOf(BackendChannelProvider::class.java, "frontChannel")
    }
}
