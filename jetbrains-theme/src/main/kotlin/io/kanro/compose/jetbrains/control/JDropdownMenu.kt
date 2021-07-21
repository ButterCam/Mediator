package io.kanro.compose.jetbrains.control

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import kotlin.math.min

@Composable
fun JContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded) return

    Popup(
        focusable = true,
        onDismissRequest = onDismissRequest,
        contextMenu = true,
        popupPositionProvider = ContextMenuPositionProvider(offset, LocalDensity.current)
    ) {
        Column(
            modifier = modifier.background(JBTheme.panelColors.bgDialog)
                .jBorder(1.dp, JBTheme.panelColors.border)
                .width(IntrinsicSize.Max),
            content = content
        )
    }
}

@Composable
fun JContextMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClickLabel: String? = null,
    role: Role? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var hover by remember {
        mutableStateOf(false)
    }

    Box(
        modifier.height(22.dp).fillMaxWidth().clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = true,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        ).pointerMoveFilter(
            onEnter = {
                hover = true
                false
            },
            onExit = {
                hover = false
                false
            }
        ).let {
            if (hover) {
                it.background(JBTheme.selectionColors.active)
            } else {
                it
            }
        }
    ) {
        SelectionScope(hover) {
            content()
        }
    }
}

private data class ContextMenuPositionProvider(
    val contentOffset: DpOffset,
    val density: Density
) : PopupPositionProvider {

    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        return with(density) {
            val popupRect = IntRect(
                IntOffset(
                    contentOffset.x.roundToPx(),
                    contentOffset.y.roundToPx()
                ),
                popupContentSize
            ).run {
                translate(
                    min(0, windowSize.width - right),
                    min(0, windowSize.height - bottom),
                )
            }
            popupRect.topLeft
        }
    }
}
