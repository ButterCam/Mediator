package io.kanro.mediator.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.control.JPanel
import io.kanro.compose.jetbrains.control.jBorder
import io.kanro.mediator.desktop.ui.CallList
import io.kanro.mediator.desktop.ui.CallView
import io.kanro.mediator.desktop.ui.FilterBox
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import java.awt.Window
import kotlin.system.exitProcess

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    application {
        val vm = remember { MainViewModel() }

        Window(
            title = "Mediator",
            state = rememberWindowState(),
            onCloseRequest = {
                vm.recoding.value = false
                vm.serverManager?.stop()
                vm.serverManager = null
                exitApplication()
                exitProcess(0)
            }
        ) {

            CompositionLocalProvider(
                LocalWindow provides window,
                LocalMainViewModel provides vm
            ) {
                JBTheme {
                    JPanel(Modifier.fillMaxSize().jBorder(top = 1.dp, color = JBTheme.panelColors.border)) {
                        Column {
                            CallList(vm, Modifier.fillMaxWidth().height(0.dp).weight(1f))
                            FilterBox(vm, Modifier.jBorder(vertical = 1.dp, color = JBTheme.panelColors.border))
                            val call by vm.selectedCall
                            CallView(call)
                        }
                    }
                }
            }
        }
    }
}

val LocalWindow: ProvidableCompositionLocal<Window> = compositionLocalOf {
    error("CompositionLocal LocalWindow not provided")
}

val LocalMainViewModel: ProvidableCompositionLocal<MainViewModel> = compositionLocalOf {
    error("CompositionLocal MainViewModel not provided")
}
