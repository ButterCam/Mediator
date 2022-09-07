package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import io.kanro.mediator.desktop.model.ProtobufSchemaSource
import io.kanro.mediator.desktop.model.ServerRule

class ServerRuleViewModel(
    name: String = "unnamed",
    enabled: Boolean = false,
    regex: String = "",
    replaceEnabled: Boolean = true,
    replace: String = "",
    schemaSource: ProtobufSchemaSource = ProtobufSchemaSource.SERVER_REFLECTION,
    reflectionMetadata: Map<String, String> = mapOf(),
    roots: List<String> = listOf(),
    descriptors: List<String> = listOf()
) {
    constructor(serverRule: ServerRule) : this(
        serverRule.name,
        serverRule.enabled,
        serverRule.authority.toString(),
        serverRule.replaceEnabled,
        serverRule.replace,
        serverRule.schemaSource,
        serverRule.metadata,
        serverRule.roots,
        serverRule.descriptors
    )

    val name = mutableStateOf(name)

    val enabled = mutableStateOf(enabled)

    val regex = mutableStateOf(regex)

    val replaceEnabled = mutableStateOf(replaceEnabled)

    val replace = mutableStateOf(replace)

    val replaceSsl = mutableStateOf(false)

    val schemaSource = mutableStateOf(schemaSource)

    val reflectionMetadata = reflectionMetadata.map {
        MetadataEntry(it.key, it.value)
    }.toMutableStateList()

    val roots = roots.toMutableStateList()

    val descriptors = descriptors.toMutableStateList()

    fun serialize(): ServerRule {
        return ServerRule(
            name.value,
            enabled.value,
            regex.value.toRegex(),
            replaceEnabled.value,
            replace.value,
            replaceSsl.value,
            schemaSource.value,
            reflectionMetadata.filter { it.key.value.isNotEmpty() }.associate { it.key.value to it.value.value },
            roots.filter { it.isNotEmpty() },
            descriptors.filter { it.isNotEmpty() }
        )
    }
}

class MetadataEntry(key: String = "", value: String = "") {
    val key = mutableStateOf(key)

    val value = mutableStateOf(value)
}
