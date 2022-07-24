package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.toMutableStateList
import io.kanro.compose.jetbrains.JBThemeStyle
import io.kanro.mediator.desktop.model.MediatorConfiguration

class ConfigViewModel(
    theme: JBThemeStyle?,
    proxyPort: Int,
    serverRules: List<ServerRuleViewModel>,
    requestRules: List<RequestRuleViewModel>
) {
    val proxyPort = mutableStateOf(proxyPort.toString(), policy = neverEqualPolicy())

    val theme = mutableStateOf(theme, policy = neverEqualPolicy())

    val changed = mutableStateOf(false)

    val serverRules = serverRules.toMutableStateList()

    val requestRules = requestRules.toMutableStateList()

    fun serialize(): MediatorConfiguration {
        return MediatorConfiguration(
            theme.value,
            proxyPort.value.toInt(),
            serverRules.filter { it.regex.value.isNotEmpty() }.map { it.serialize() },
            requestRules.map { it.serialize() }
        )
    }
}
