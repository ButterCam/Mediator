package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.control.ActionButton
import io.kanro.compose.jetbrains.control.ActionButtonDefaults
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JBToolBar
import io.kanro.compose.jetbrains.control.TextField
import io.kanro.compose.jetbrains.control.TextFieldDefaults
import io.kanro.compose.jetbrains.control.jBorder
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.internal.ServerManager
import java.io.IOException
import java.net.BindException

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun FilterBox(modifier: Modifier = Modifier) {
    Row(modifier.height(28.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        var filter by MainViewModel.filter
        val calls = MainViewModel.calls
        val shownCalls = MainViewModel.shownCalls

        TextField(
            value = filter,
            onValueChange = {
                filter = it
                shownCalls.clear()
                shownCalls += calls.filter {
                    val start = it.start()
                    filter.isEmpty() || start.authority.contains(filter) || start.method.contains(filter)
                }
            },
            modifier = Modifier.width(0.dp).weight(2f).fillMaxHeight()
                .jBorder(end = 1.dp, color = JBTheme.panelColors.border),
            leadingIcon = {
                Icon("icons/search.svg")
            },
            trailingIcon = if (filter.isEmpty()) null else {
                {
                    ClearButton({
                        filter = ""
                    })
                }
            },
            style = TextFieldDefaults.textFieldStyle(
                indicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                borderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
            )
        )
        Box(Modifier.width(0.dp).weight(1f), contentAlignment = Alignment.CenterEnd) {
            JBToolBar(Orientation.Horizontal, modifier = Modifier.padding(end = 7.dp)) {
                var configWindow by remember { mutableStateOf(false) }

                ActionButton({
                    if (MainViewModel.recoding.value) {
                        MainViewModel.recoding.value = false
                    } else {
                        if (MainViewModel.serverManager == null) {
                            MainViewModel.serverManager = try {
                                ServerManager(MainViewModel, MainViewModel.configuration).run()
                            } catch (e: IOException) {
                                when (val cause = e.cause) {
                                    is BindException -> {
                                        throw e
                                    }
                                    else -> throw e
                                }
                            }
                        }
                        MainViewModel.recoding.value = true
                    }
                }) {
                    if (MainViewModel.recoding.value) {
                        Icon("icons/suspend.svg", "Stop server")
                    } else {
                        Icon("icons/run.svg", "Start server")
                    }
                }
                ActionButton({
                    calls.clear()
                    shownCalls.clear()
                }) {
                    Icon("icons/clear.svg", "Clear log")
                }
                ActionButton({
                    configWindow = true
                }) {
                    Icon("icons/editorconfig.svg", "Configuration")
                    if (configWindow) {
                        ConfigDialog(
                            MainViewModel.configView(),
                            {
                                MainViewModel.configuration = it.serialize().also {
                                    it.save()
                                }
                                MainViewModel.serverManager?.stop()
                                MainViewModel.serverManager = null
                                MainViewModel.serverManager =
                                    ServerManager(MainViewModel, MainViewModel.configuration).run()
                            },
                            {
                                configWindow = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClearButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    background: Color = Color.Transparent,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(3.dp),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ActionButtonDefaults.ContentPadding,
) {
    ActionButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = background,
        indication = null,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        contentPadding = contentPadding
    ) {
        val hovering by interactionSource.collectIsHoveredAsState()
        if (hovering) {
            Icon("icons/deleteTagHover.svg")
        } else {
            Icon("icons/close.svg")
        }
    }
}
