package io.kanro.mediator.proxy

import io.netty.channel.Channel

interface ChannelInitializer<T : Channel> {
    fun init(ch: T)
}
