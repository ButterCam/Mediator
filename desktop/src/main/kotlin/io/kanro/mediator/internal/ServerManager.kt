package io.kanro.mediator.internal

import com.bybutter.sisyphus.protobuf.ProtobufBooster
import io.grpc.ConnectivityState
import io.grpc.Grpc
import io.grpc.HttpConnectProxiedSocketAddress
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.TlsChannelCredentials
import io.kanro.mediator.desktop.model.MediatorConfiguration
import io.kanro.mediator.desktop.model.ProtobufSchemaSource
import io.kanro.mediator.desktop.model.ServerRule
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.netty.frontend.GrpcProxyServer
import java.net.InetSocketAddress
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class ServerManager(vm: MainViewModel, private val config: MediatorConfiguration) {
    /**
     * 0 for waiting, 1 for running, 2 for stopped
     */
    private var state = 0

    private val interceptor = MediatorGrpcProxySupport(config, vm.sslConfig)

    private val proxyServer = GrpcProxyServer(config.proxyPort, interceptor)

    private val channels = mutableMapOf<String, ManagedChannel>()

    private val replayChannels = mutableMapOf<String, ManagedChannel>()

    private val reflections = mutableMapOf<String, MediatorProtoReflection>()

    fun rule(authority: String): ServerRule? {
        return config.serverRules.filter {
            it.authority.matches(authority) && it.enabled
        }.firstOrNull()
    }

    fun channel(authority: String, ssl: Boolean): io.grpc.Channel {
        val rule = rule(authority)

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
            if (ssl) {
                val creds = TlsChannelCredentials.newBuilder().trustManager(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit

                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                }).build()
                Grpc.newChannelBuilder(authority, creds).proxyDetector {
                    HttpConnectProxiedSocketAddress.newBuilder().setTargetAddress(it as InetSocketAddress)
                        .setProxyAddress(
                            InetSocketAddress(
                                "127.0.0.1",
                                config.proxyPort,
                            ),
                        ).build()
                }.build()
            } else {
                ManagedChannelBuilder.forTarget(authority).proxyDetector {
                    HttpConnectProxiedSocketAddress.newBuilder().setTargetAddress(it as InetSocketAddress)
                        .setProxyAddress(
                            InetSocketAddress(
                                "127.0.0.1",
                                config.proxyPort,
                            ),
                        ).build()
                }.usePlaintext().build()
            }.apply {
                this.notifyWhenStateChanged(
                    ConnectivityState.CONNECTING,
                    {
                        println("Connecting to $authority")
                    },
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.IDLE,
                    {
                        println("Connected to $authority")
                    },
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.READY,
                    {
                        println("$authority ready")
                    },
                )
                this.notifyWhenStateChanged(
                    ConnectivityState.TRANSIENT_FAILURE,
                    {
                        println("File to connect to $authority")
                    },
                )
            }
        }
    }

    fun reflection(authority: String, ssl: Boolean): MediatorProtoReflection {
        return reflections.getOrPut("http${if (ssl) "s" else ""}://$authority") {
            val rule = rule(authority)

            when (rule?.schemaSource) {
                ProtobufSchemaSource.PROTO_ROOT -> ProtoRootReflection(
                    rule.roots,
                )

                ProtobufSchemaSource.FILE_DESCRIPTOR_SET -> FileDescriptorSetReflection(
                    rule.descriptors,
                )

                else -> ServerProtoReflection(channel(authority, ssl)).apply {
                    ProtobufBooster.boost(this)
                }
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
