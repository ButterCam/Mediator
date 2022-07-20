package io.kanro.mediator.desktop.model

import com.bybutter.sisyphus.jackson.parseJson
import com.bybutter.sisyphus.jackson.toJson
import com.github.fge.jackson.jsonpointer.JsonPointer
import com.github.fge.jsonpatch.AddOperation
import com.github.fge.jsonpatch.CopyOperation
import com.github.fge.jsonpatch.JsonPatch
import com.github.fge.jsonpatch.MoveOperation
import com.github.fge.jsonpatch.RemoveOperation
import com.github.fge.jsonpatch.ReplaceOperation
import io.kanro.compose.jetbrains.JBThemeStyle
import net.harawata.appdirs.AppDirsFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class MediatorConfiguration(
    val theme: JBThemeStyle? = null,
    val proxyPort: Int = 8888,
    val grpcPort: Int = 9999,
    val serverRules: List<ServerRule> = listOf(),
    val requestRules: List<RequestRule> = listOf(),
) {
    companion object {
        private val configDir = Path(
            AppDirsFactory.getInstance()
                .getUserConfigDir("mediator", "1.0", "higan")
        )

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
    val metadata: Map<String, String>,
)

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
        REQUEST_METADATA,
        INPUT,
        RESPONSE_METADATA,
        OUTPUT,
        TRAILER,
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
