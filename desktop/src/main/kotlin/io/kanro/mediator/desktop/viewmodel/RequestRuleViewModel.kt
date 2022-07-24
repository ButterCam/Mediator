package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateOf
import io.kanro.mediator.desktop.model.RequestRule

class RequestRuleViewModel(
    name: String = "unnamed",
    enabled: Boolean = false,
    method: String = "",
    type: RequestRule.Type = RequestRule.Type.INPUT,
    op: RequestRule.Operation = RequestRule.Operation.REPLACE,
    path: String = "/",
    value: String = ""
) {
    constructor(rule: RequestRule) : this(
        rule.name,
        rule.enabled,
        rule.method,
        rule.type,
        rule.op,
        rule.path,
        rule.value
    )

    val name = mutableStateOf(name)

    val enabled = mutableStateOf(enabled)

    val method = mutableStateOf(method)

    val type = mutableStateOf(type)

    val op = mutableStateOf(op)

    val path = mutableStateOf(path)

    val value = mutableStateOf(value)

    fun serialize(): RequestRule {
        return RequestRule(
            name.value,
            enabled.value,
            method.value,
            type.value,
            op.value,
            path.value,
            value.value,
        )
    }
}
