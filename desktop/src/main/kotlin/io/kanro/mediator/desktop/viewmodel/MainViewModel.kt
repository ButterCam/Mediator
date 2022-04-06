package io.kanro.mediator.desktop.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.MediatorConfiguration
import io.kanro.mediator.internal.ServerManager

class MainViewModel {
    val recoding = mutableStateOf(true)

    val filter = mutableStateOf("")

    val calls = mutableListOf<CallTimeline>()

    val shownCalls = mutableStateListOf<CallTimeline>()

    val selectedCall = mutableStateOf<CallTimeline?>(null)

    var configuration: MediatorConfiguration = MediatorConfiguration.load()

    var serverManager: ServerManager? = ServerManager(this, configuration).run()

    fun configView(): ConfigViewModel {
        return ConfigViewModel(
            configuration.proxyPort,
            configuration.grpcPort,
            configuration.serverRules.map {
                ServerRuleViewModel(it)
            },
            configuration.requestRules.map {
                RequestRuleViewModel(it)
            }
        )
    }
}
