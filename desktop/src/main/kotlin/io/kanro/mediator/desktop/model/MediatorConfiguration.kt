package io.kanro.mediator.desktop.model

import com.bybutter.sisyphus.jackson.parseJson
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.security.base64
import com.bybutter.sisyphus.security.base64Decode
import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.AddOperation
import com.github.fge.jsonpatch.CopyOperation
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.MoveOperation
import com.github.fge.jsonpatch.RemoveOperation
import com.github.fge.jsonpatch.ReplaceOperation
import io.kanro.compose.jetbrains.JBThemeStyle
import io.kanro.mediator.netty.ssl.generateCertificate
import io.kanro.mediator.netty.ssl.generateKeyPair
import io.kanro.mediator.netty.ssl.generateServerCertificate
import net.harawata.appdirs.AppDirsFactory
import java.security.KeyFactory
import java.security.KeyPair
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

private val configDir = Path(
    AppDirsFactory.getInstance().getUserConfigDir("mediator", "1.0", "higan")
)

data class MediatorSslConfig(
    val publicKey: String,
    val privateKey: String,
    val ca: String,
) {
    companion object {
        private val configFile = configDir.resolve("ssl.json")

        val instance: MediatorSslConfig by lazy {
            configDir.createDirectories()
            if (configFile.exists()) {
                configFile.readText().parseJson()
            } else {
                val keyPair = generateKeyPair()
                val cert = generateCertificate(keyPair)
                MediatorSslConfig(
                    keyPair.public.encoded.base64(), keyPair.private.encoded.base64(), cert.encoded.base64()
                ).apply {
                    configDir.createDirectories()
                    configFile.writeText(toJson())
                }
            }
        }
    }

    @get:JsonIgnore
    val caKeyPair: KeyPair by lazy {
        val pub = X509EncodedKeySpec(publicKey.base64Decode())
        val prv = PKCS8EncodedKeySpec(privateKey.base64Decode())

        val public = KeyFactory.getInstance("RSA").generatePublic(pub)
        val private = KeyFactory.getInstance("RSA").generatePrivate(prv)

        KeyPair(public, private)
    }

    @get:JsonIgnore
    val caRoot: X509Certificate by lazy {
        CertificateFactory.getInstance("X.509").generateCertificate(ca.base64Decode().inputStream()) as X509Certificate
    }

    @JsonIgnore
    private val certPool: MutableMap<String, Pair<X509Certificate, KeyPair>> = ConcurrentHashMap()

    fun getCert(name: String): Pair<X509Certificate, KeyPair> {
        val host = if (name.count { it == '.' } < 2) {
            name
        } else {
            name.substringAfter('.')
        }

        return certPool.getOrPut(host) {
            val keyPair = generateKeyPair()
            val cert = generateServerCertificate(
                "*.$host",
                listOf(
                    host, "*.$host"
                ),
                keyPair, caRoot, caKeyPair
            )
            cert to keyPair
        }
    }
}

data class MediatorConfiguration(
    val theme: JBThemeStyle? = null,
    val proxyPort: Int = 8888,
    val serverRules: List<ServerRule> = listOf(),
    val requestRules: List<RequestRule> = listOf(),
) {
    companion object {
        private val configFile = configDir.resolve("config.json")

        fun load(): MediatorConfiguration {
            configDir.createDirectories()
            return if (configFile.exists()) {
                configFile.readText().parseJson()
            } else {
                MediatorConfiguration()
            }
        }
    }

    fun save() {
        configDir.createDirectories()
        configFile.writeText(toJson())
    }
}

data class ServerRule(
    val name: String,
    val enabled: Boolean,
    val authority: Regex,
    val replaceEnabled: Boolean = true,
    val replace: String,
    val replaceSsl: Boolean = false,
    val schemaSource: ProtobufSchemaSource = ProtobufSchemaSource.SERVER_REFLECTION,
    val metadata: Map<String, String> = mapOf(),
    val roots: List<String> = listOf(),
    val descriptors: List<String> = listOf()
)

enum class ProtobufSchemaSource {
    SERVER_REFLECTION,
    PROTO_ROOT,
    FILE_DESCRIPTOR_SET,
}

data class RequestRule(
    val name: String,
    val enabled: Boolean,
    val method: String,
    val type: Type,
    val op: Operation,
    val path: String,
    val value: String,
) {
    enum class Type {
        REQUEST_METADATA, INPUT, RESPONSE_METADATA, OUTPUT, TRAILER,
    }

    enum class Operation {
        ADD, REMOVE, REPLACE, COPY, MOVE, TEST
    }

    private val patch: JsonPatch by lazy {
        val operation = when (op) {
            Operation.ADD -> AddOperation(JsonPointer(path), value.parseJson())
            Operation.REMOVE -> RemoveOperation(JsonPointer(path))
            Operation.REPLACE -> ReplaceOperation(JsonPointer(path), value.parseJson())
            Operation.COPY -> CopyOperation(JsonPointer(path), JsonPointer(value))
            Operation.MOVE -> MoveOperation(JsonPointer(path), JsonPointer(value))
            Operation.TEST -> ReplaceOperation(JsonPointer(path), value.parseJson())
        }
        JsonPatch(listOf(operation))
    }

    fun toPatch(): JsonPatch {
        return patch
    }
}
