package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import io.kanro.mediator.desktop.model.RequestPatcher
import io.kanro.mediator.desktop.model.RequestRule

class RequestRuleViewModel(
    name: String = "unnamed",
    enabled: Boolean = false,
    method: String = "",
    inputPatchers: List<RequestPatcher> = emptyList(),
    outputPatchers: List<RequestPatcher> = emptyList()
) {
    constructor(rule: RequestRule) : this(
        rule.name,
        rule.enabled,
        rule.method,
        rule.inputPatchers,
        rule.outputPatchers
    )

    val name = mutableStateOf(name)

    val enabled = mutableStateOf(enabled)

    val method = mutableStateOf(method)

    val inputPatchers = inputPatchers.map {
        PatcherViewModel(it.enabled, it.body, it.removedFields, it.metadata)
    }.toMutableStateList()

    val outputPatchers = outputPatchers.map {
        PatcherViewModel(it.enabled, it.body, it.removedFields, it.metadata)
    }.toMutableStateList()

    fun serialize(): RequestRule {
        return RequestRule(
            name.value,
            enabled.value,
            method.value,
            inputPatchers.map { it.serialize() },
            outputPatchers.map { it.serialize() }
        )
    }
}

class PatcherViewModel(
    enabled: Boolean = false,
    input: String = "",
    removedFields: List<String> = listOf(),
    metadata: Map<String, String> = mapOf()
) {
    val enabled = mutableStateOf(enabled)

    val input = mutableStateOf(input)

    val removedFields = removedFields.toMutableStateList()

    val metadata = metadata.map {
        MetadataEntry(it.key, it.value)
    }.toMutableStateList()

    fun serialize(): RequestPatcher {
        return RequestPatcher(
            enabled.value,
            input.value,
            removedFields.toList(),
            metadata.filter { it.key.value.isNotEmpty() }.associate { it.key.value to it.value.value }
        )
    }
}
