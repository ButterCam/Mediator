package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bybutter.sisyphus.protobuf.primitives.minus
import com.bybutter.sisyphus.protobuf.primitives.toTime
import io.grpc.Status
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JPanelBorder
import io.kanro.compose.jetbrains.control.ListItemHoverIndication
import io.kanro.compose.jetbrains.control.Text
import io.kanro.compose.jetbrains.control.jBorder
import io.kanro.mediator.desktop.model.CallEvent
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.asState
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import java.awt.Cursor
import java.util.concurrent.TimeUnit

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun CallList(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        val listState = rememberLazyListState()
        val calls = MainViewModel.shownCalls
        var selected by MainViewModel.selectedCall
        var requestToEnd by remember { mutableStateOf(false) }

        var idWidth by remember { mutableStateOf(50.dp) }
        var authorityWidth by remember { mutableStateOf(180.dp) }
        var costWidth by remember { mutableStateOf(75.dp) }

        if (calls.isEmpty()) {
            Text("Wait for incoming request...", Modifier.align(Alignment.Center))
        } else {
            val itemCount = listState.layoutInfo.totalItemsCount

            LaunchedEffect(calls.size) {
                if (requestToEnd) {
                    listState.scrollToItem(itemCount - 1)
                }
            }

            LazyColumn(state = listState) {
                stickyHeader {
                    CallRowHeader(idWidth, authorityWidth, costWidth) { index, delta ->
                        when (index) {
                            0 -> {
                                idWidth += delta.dp
                            }

                            1 -> {
                                authorityWidth += delta.dp
                            }

                            2 -> {
                                costWidth -= delta.dp
                            }
                        }
                    }
                }

                items(calls) { x ->
                    CallRow(idWidth, authorityWidth, costWidth, x, selected == x) {
                        selected = x
                    }
                }

                if (itemCount == calls.size) {
                    requestToEnd = listState.isScrolledToTheEnd()
                }
            }

            VerticalScrollbar(
                rememberScrollbarAdapter(listState),
                Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

fun LazyListState.isScrolledToTheEnd() =
    layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun CallRowHeader(
    idWidth: Dp,
    authorityWidth: Dp,
    costWidth: Dp,
    resizing: (Int, Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .height(23.dp)
            .fillMaxWidth()
            .background(JBTheme.panelColors.bgContent)
            .jBorder(bottom = 1.dp, color = JBTheme.panelColors.border),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val idState = rememberDraggableState { delta ->
            resizing(0, delta)
        }
        val authorityState = rememberDraggableState { delta ->
            resizing(1, delta)
        }
        val costState = rememberDraggableState { delta ->
            resizing(2, delta)
        }

        fun Modifier.resizeable(state: DraggableState): Modifier {
            return pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = state
                )
        }

        Spacer(Modifier.width(23.dp))
        JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
        Box(Modifier.width(idWidth)) {
            Text("ID", Modifier.padding(start = 7.dp))
        }
        JPanelBorder(Modifier.width(1.dp).fillMaxHeight().resizeable(idState))
        Box(Modifier.width(authorityWidth)) {
            Text("Authority", Modifier.padding(start = 7.dp))
        }
        JPanelBorder(Modifier.width(1.dp).fillMaxHeight().resizeable(authorityState))
        Box(Modifier.width(0.dp).weight(1f)) {
            Text("Method", Modifier.padding(start = 7.dp))
        }
        JPanelBorder(Modifier.width(1.dp).fillMaxHeight().resizeable(costState))
        Box(Modifier.width(costWidth)) {
            Text("Cost", Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
fun CallRow(
    idWidth: Dp,
    authorityWidth: Dp,
    costWidth: Dp,
    record: CallTimeline,
    selected: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = null,
    onClick: () -> Unit
) {
    SelectionScope(selected) {
        Row(
            modifier = Modifier
                .height(23.dp)
                .fillMaxWidth()
                .run {
                    if (!selected) {
                        background(color = JBTheme.panelColors.bgContent)
                    } else {
                        this
                    }
                }
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ListItemHoverIndication,
                    onClick = onClick,
                    role = role
                )
                .run {
                    if (selected) {
                        background(color = JBTheme.selectionColors.active)
                    } else {
                        this
                    }
                }
                .hoverable(interactionSource)
                .jBorder(bottom = 1.dp, color = JBTheme.panelColors.border),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val call by record.asState()
            val start = call.start()

            CallIcon(call)
            JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
            Box(Modifier.width(idWidth)) {
                Text(call.id, Modifier.padding(start = 7.dp), maxLines = 1)
            }
            JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
            Row(Modifier.width(authorityWidth)) {
                Spacer(Modifier.width(7.dp))
                if (start != null) {
                    val rewriteMark = if (start.authority == start.resolvedAuthority) {
                        ""
                    } else {
                        "*"
                    }
                    if (start.ssl) {
                        Icon("icons/ssl.svg")
                    }
                    Text(start.authority + rewriteMark, maxLines = 1)
                } else {
                    Text(call.first<CallEvent.Transparent>()!!.authority, maxLines = 1)
                }
            }
            JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
            Box(Modifier.width(0.dp).weight(1f)) {
                if (start != null) {
                    Text(start.method, Modifier.padding(start = 7.dp), maxLines = 1)
                } else {
                    Text("-", Modifier.padding(start = 7.dp), maxLines = 1)
                }
            }
            JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
            Box(Modifier.width(costWidth)) {
                if (start != null) {
                    Text(call.cost(), Modifier.padding(start = 7.dp), maxLines = 1)
                } else {
                    Text("-", Modifier.padding(start = 7.dp), maxLines = 1)
                }
            }
        }
    }
}

fun CallTimeline.cost(): String {
    val start = start()!!
    val close = close() ?: return "Running..."
    val duration = close.timestamp() - start.timestamp()
    val cost = duration.toTime(TimeUnit.NANOSECONDS)
    return when {
        cost < TimeUnit.MICROSECONDS.toNanos(1) -> {
            "${cost}ns"
        }

        cost < TimeUnit.MILLISECONDS.toNanos(1) -> {
            "${cost / 1000.0}µs"
        }

        cost < TimeUnit.SECONDS.toNanos(1) -> {
            String.format("%.3fms", cost / 1000000.0)
        }

        else -> {
            String.format("%.3fs", cost / 1000000000.0)
        }
    }
}

@Composable
fun CallIcon(call: CallTimeline, modifier: Modifier = Modifier) {
    val status = call.close()?.status
    val start = call.start()
    Box(Modifier.size(23.dp), contentAlignment = Alignment.Center) {
        if (start == null) {
            Icon("icons/ssl_undecoded.svg")
        } else {
            when (status?.code) {
                Status.Code.OK -> Icon("icons/toolbarPassed.svg")

                Status.Code.ABORTED,
                Status.Code.CANCELLED -> Icon("icons/toolbarTerminated.svg")

                Status.Code.DEADLINE_EXCEEDED,
                Status.Code.PERMISSION_DENIED,
                Status.Code.UNAUTHENTICATED -> Icon("icons/toolbarSkipped.svg")

                Status.Code.ALREADY_EXISTS,
                Status.Code.RESOURCE_EXHAUSTED,
                Status.Code.FAILED_PRECONDITION,
                Status.Code.OUT_OF_RANGE,
                Status.Code.UNAVAILABLE,
                Status.Code.INVALID_ARGUMENT -> Icon("icons/toolbarFailed.svg")

                Status.Code.DATA_LOSS,
                Status.Code.NOT_FOUND -> Icon("icons/testUnknown.svg")

                Status.Code.UNIMPLEMENTED,
                Status.Code.UNKNOWN,
                Status.Code.INTERNAL -> Icon("icons/toolbarError.svg")

                else -> Icon("icons/run.svg")
            }
        }
    }
}
