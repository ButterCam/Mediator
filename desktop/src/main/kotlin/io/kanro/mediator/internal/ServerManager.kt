package io.kanro.mediator.internal

import com.bybutter.sisyphus.protobuf.ProtobufBooster
import io.grpc.ConnectivityState
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.kanro.mediator.desktop.model.MediatorConfiguration
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.netty.frontend.GrpcProxyServer
import java.net.InetSocketAddress

class ServerManager(vm: MainViewModel, private val config: MediatorConfiguration) {
    /**
     * 0 for waiting, 1 for running, 2 for stopped
     */
    private var state = 0

    private val interceptor = MediatorGrpcProxySupport(config, vm.sslConfig)

    private val proxyServer = GrpcProxyServer(config.proxyPort, interceptor)

    private val channels = mutableMapOf<String, ManagedChannel>()

    private val replayChannels = mutableMapOf<String, ManagedChannel>()

    private val reflections = mutableMapOf<String, StatefulProtoReflection>()

    fun channel(authority: String, ssl: Boolean): io.grpc.Channel {
        val rule = config.serverRules.filter {
            it.authority.matches(authority) && it.enabled
        }.firstOrNull()

        val rewriteAuthority = rule?.takeIf { it.replaceEnabled && it.replace.isNotEmpty() }?.replace ?: authority

        val rewriteEnable = rule?.replaceEnabled == true && rule.replace.isNotEmpty()
        val rewriteBackendSsl = rule?.replaceSsl ?: false

        return channels.getOrPut("http${if (ssl) "s" else ""}://$rewriteAuthority") {
            ManagedChannelBuilder.forTarget(rewriteAuthority)
                .intercept(ReflectionAuth(rule))
                .apply {
                    if ((rewriteEnable && rewriteBackendSsl) || (!rewriteEnable && ssl)) {
                        useTransportSecurity()
                    } else {
                        usePlaintext()
                    }
                }
                .build()
        }
    }

    fun replayChannel(authority: String, ssl: Boolean): io.grpc.Channel {
        return replayChannels.getOrPut("http${if (ssl) "s" else ""}://$authority") {
            ManagedChannelBuilder.forTarget(authority).proxyDetector {
                HttpConnectProxiedSocketAddress.newBuilder().setTargetAddress(it as InetSocketAddress).setProxyAddress(
                    InetSocketAddress(
                        "127.0.0.1", config.proxyPort
                    )
                ).build()
            }.apply {
                if (ssl) {
                    useTransportSecurity()
                } else {
                    usePlaintext()
                }
            }.build().apply {
                this.notifyWhenStateChanged(
                    ConnectivityState.CONNECTING,
                    {
                        println("Connecting to $authority")
                    }
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.IDLE,
                    {
                        println("Connected to $authority")
                    }
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.READY,
                    {
                        println("$authority ready")
                    }
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.TRANSIENT_FAILURE,
                    {
                        println("File to connect to $authority")
                    }
                )
            }
        }
    }

    fun reflection(authority: String, ssl: Boolean): StatefulProtoReflection {
        return reflections.getOrPut("http${if (ssl) "s" else ""}://$authority") {
            StatefulProtoReflection(channel(authority, ssl)).apply {
                ProtobufBooster.boost(this)
            }
        }
    }

    fun run(): ServerManager {
        if (state != 0) {
            throw IllegalStateException("Wrong state for running")
        }
        proxyServer.run()
        state = 1
        return this
    }

    fun stop(): ServerManager {
        if (state != 1) {
            throw IllegalStateException("Wrong state for running")
        }
        proxyServer.close()
        channels.values.forEach {
            it.shutdownNow()
        }
        replayChannels.values.forEach {
            it.shutdownNow()
        }
        state = 2
        return this
    }
}
