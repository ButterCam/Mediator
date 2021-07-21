package io.kanro.mediator.internal

import io.grpc.Channel
import io.kanro.mediator.grpc.BasicProxyRegistry

class MediatorRegistry(private val serverManager: ServerManager) : BasicProxyRegistry() {
    override fun targetChannel(methodName: String, authority: String?): Channel {
        return serverManager.channel(authority!!)
    }
}
