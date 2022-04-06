package io.kanro.mediator.desktop.model

import com.bybutter.sisyphus.jackson.parseJson
import com.bybutter.sisyphus.jackson.toJson
import net.harawata.appdirs.AppDirsFactory
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

data class MediatorConfiguration(
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
    val inputPatchers: List<RequestPatcher>,
    val outputPatchers: List<RequestPatcher>,
)

data class RequestPatcher(
    val enabled: Boolean,
    val body: String,
    val removedFields: List<String>,
    val metadata: Map<String, String>,
)