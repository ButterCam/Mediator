package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import io.kanro.compose.jetbrains.control.ActionButton
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JBToolBar

@Composable
fun <T> ListToolBar(
    orientation: Orientation,
    list: List<T>,
    selectedItem: T?,
    onCreate: (() -> Unit)? = null,
    onRemove: ((T?) -> Unit)? = null,
    onMoveUp: ((T?) -> Unit)? = null,
    onMoveDown: ((T?) -> Unit)? = null,
    content: @Composable (() -> Unit)? = null
) {
    JBToolBar(
        orientation
    ) {
        if (onCreate != null) {
            ActionButton(onCreate) {
                Icon("icons/add.svg")
            }
        }
        if (onRemove != null) {
            ActionButton(
                {
                    onRemove(selectedItem)
                },
                enabled = selectedItem != null
            ) {
                Icon("icons/remove.svg")
            }
        }
        if (onMoveUp != null) {
            ActionButton(
                {
                    onMoveUp(selectedItem)
                },
                enabled = selectedItem != null && selectedItem != list.first()
            ) {
                Icon("icons/arrowUp.svg")
            }
        }
        if (onMoveDown != null) {
            ActionButton(
                {
                    onMoveDown(selectedItem)
                },
                enabled = selectedItem != null && selectedItem != list.last()
            ) {
                Icon("icons/arrowDown.svg")
            }
        }

        content?.invoke()
    }
}