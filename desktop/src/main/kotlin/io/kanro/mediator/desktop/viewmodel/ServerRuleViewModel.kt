package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import io.kanro.mediator.desktop.model.ServerRule

class ServerRuleViewModel(
    name: String = "unnamed",
    enabled: Boolean = false,
    regex: String = "",
    replaceEnabled: Boolean = true,
    replace: String = "",
    reflectionMetadata: Map<String, String> = mapOf()
) {
    constructor(serverRule: ServerRule) : this(
        serverRule.name,
        serverRule.enabled,
        serverRule.authority.toString(),
        serverRule.replaceEnabled,
        serverRule.replace,
        serverRule.metadata
    )

    val name = mutableStateOf(name)

    val enabled = mutableStateOf(enabled)

    val regex = mutableStateOf(regex)

    val replaceEnabled = mutableStateOf(replaceEnabled)

    val replace = mutableStateOf(replace)

    val replaceSsl = mutableStateOf(false)

    val reflectionMetadata = reflectionMetadata.map {
        MetadataEntry(it.key, it.value)
    }.toMutableStateList()

    fun serialize(): ServerRule {
        return ServerRule(
            name.value,
            enabled.value,
            regex.value.toRegex(),
            replaceEnabled.value,
            replace.value,
            replaceSsl.value,
            reflectionMetadata.filter { it.key.value.isNotEmpty() }.associate { it.key.value to it.value.value }
        )
    }
}

class MetadataEntry(key: String = "", value: String = "") {
    val key = mutableStateOf(key)

    val value = mutableStateOf(value)
}
