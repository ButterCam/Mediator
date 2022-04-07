package io.kanro.mediator.internal

import com.bybutter.sisyphus.protobuf.ProtobufBooster
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Server
import io.grpc.ServerBuilder
import io.kanro.mediator.desktop.model.MediatorConfiguration
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.proxy.backend.BackendChannelProvider
import io.kanro.mediator.proxy.frontend.ProxyServer

class ServerManager(vm: MainViewModel, private val config: MediatorConfiguration) {
    /**
     * 0 for waiting, 1 for running, 2 for stopped
     */
    private var state = 0

    private val server: Server = ServerBuilder.forPort(config.grpcPort)
        .intercept(GrpcCallLogger())
        .fallbackHandlerRegistry(MediatorRegistry(this))
        .build()

    private val proxyServer = ProxyServer("http", config.proxyPort)

    private val channels = mutableMapOf<String, ManagedChannel>()

    private val reflections = mutableMapOf<String, StatefulProtoReflection>()

    fun channel(authority: String): io.grpc.Channel {
        val rules = config.serverRules.filter {
            it.authority.matches(authority)
        }
        val rewriteAuthority = rules.fold(authority) { r, it ->
            if (it.enabled && it.replaceEnabled && it.replace.isNotBlank()) {
                it.authority.replace(authority, it.replace)
            } else r
        }

        return channels.getOrPut(rewriteAuthority) {
            ManagedChannelBuilder.forTarget(rewriteAuthority)
                .intercept(ReflectionAuth(rules))
                .usePlaintext().build()
        }
    }

    fun reflection(authority: String): StatefulProtoReflection {
        return reflections.getOrPut(authority) {
            StatefulProtoReflection(channel(authority)).apply {
                ProtobufBooster.boost(this)
            }
        }
    }

    fun run(): ServerManager {
        if (state != 0) {
            throw IllegalStateException("Wrong state for running")
        }
        server.start()
        proxyServer.run(BackendChannelProvider("tcp", "localhost", config.grpcPort))
        state = 1
        return this
    }

    fun stop(): ServerManager {
        if (state != 1) {
            throw IllegalStateException("Wrong state for running")
        }
        proxyServer.close()
        server.shutdown().awaitTermination()
        channels.values.forEach {
            it.shutdownNow()
        }
        state = 2
        return this
    }
}
