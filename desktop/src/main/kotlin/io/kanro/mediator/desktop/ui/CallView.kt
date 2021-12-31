package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.primitives.string
import io.grpc.Metadata
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JPanelBorder
import io.kanro.compose.jetbrains.control.ListItemHoverIndication
import io.kanro.compose.jetbrains.control.ProgressBar
import io.kanro.compose.jetbrains.control.Tab
import io.kanro.compose.jetbrains.control.Text
import io.kanro.compose.jetbrains.interaction.hoverable
import io.kanro.mediator.desktop.LocalMainViewModel
import io.kanro.mediator.desktop.LocalWindow
import io.kanro.mediator.desktop.model.CallEvent
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.asState
import io.kanro.mediator.utils.toMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CallView(call: CallTimeline?) {
    var selectedTab by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(200.dp) }
    var startResize by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxWidth()) {
        JPanelBorder(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart))
        val window = LocalWindow.current

        Box(
            Modifier.matchParentSize().draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    height -= delta.dp
                },
                onDragStarted = {
                    startResize = true
                    window.cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
                },
                onDragStopped = {
                    startResize = false
                    window.cursor = Cursor.getDefaultCursor()
                }
            ).pointerMoveFilter(
                onEnter = {
                    window.cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
                    false
                },
                onExit = {
                    if (!startResize) {
                        window.cursor = Cursor.getDefaultCursor()
                    }
                    false
                }
            )
        ) {
        }
        Row(Modifier.fillMaxWidth().align(Alignment.CenterStart)) {
            Tab(
                selectedTab == 0,
                {
                    selectedTab = 0
                }
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon("icons/ppLib.svg")
                    Text("Statistics", modifier = Modifier.offset(y = (-1).dp))
                }
            }
            Tab(
                selectedTab == 1,
                {
                    selectedTab = 1
                }
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon("icons/timeline.svg")
                    Text("Timeline", modifier = Modifier.offset(y = (-1).dp))
                }
            }
        }
    }

    Box(Modifier.fillMaxWidth().height(height)) {
        if (call == null) {
            Text("Select one call to view details", Modifier.align(Alignment.Center))
        } else {
            when (selectedTab) {
                0 -> StatisticsView(call)
                1 -> TimelineView(call)
            }
        }
    }
}

@Composable
fun StatisticsView(call: CallTimeline) {
    Column(Modifier.background(Color.White).fillMaxSize()) {

        var selectedKey by remember(call) { mutableStateOf(-1) }

        MetadataItem("ID", call.id, selectedKey == 0) {
            selectedKey = 0
        }
        val start = call.start()
        MetadataItem("Authority", start.authority, selectedKey == 1) {
            selectedKey = 1
        }
        MetadataItem("Method", start.method, selectedKey == 2) {
            selectedKey = 2
        }
        MetadataItem("Start time", start.timestamp().string(), selectedKey == 3) {
            selectedKey = 3
        }
        val close = call.close()
        if (close != null) {
            MetadataItem("End time", close.timestamp().string(), selectedKey == 4) {
                selectedKey = 4
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineView(call: CallTimeline) {
    var selectedEvent by remember(call) { mutableStateOf<CallEvent?>(null) }
    var width by remember { mutableStateOf(200.dp) }
    var startResize by remember { mutableStateOf(false) }

    Row {
        val vState = rememberScrollState()
        Box {
            Column(Modifier.width(width).verticalScroll(vState)) {
                call.events().forEach {
                    TimelineItemRow(call, it, selectedEvent == it) {
                        selectedEvent = it
                    }
                }
            }

            VerticalScrollbar(rememberScrollbarAdapter(vState), Modifier.align(Alignment.CenterEnd))
        }

        val window = LocalWindow.current
        JPanelBorder(
            Modifier.width(1.dp).fillMaxHeight().draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    width += delta.dp
                },
                onDragStarted = {
                    startResize = true
                    window.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
                },
                onDragStopped = {
                    startResize = false
                    window.cursor = Cursor.getDefaultCursor()
                }
            ).pointerMoveFilter(
                onEnter = {
                    window.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
                    false
                },
                onExit = {
                    if (!startResize) {
                        window.cursor = Cursor.getDefaultCursor()
                    }
                    false
                }
            )
        )

        Box {
            val timeline by call.asState()
            if (selectedEvent == null) {
                Text("Select one event to view details", Modifier.align(Alignment.Center))
            } else {
                when (val event = selectedEvent) {
                    is CallEvent.Accept -> EventView(event)
                    is CallEvent.Close -> EventView(event)
                    is CallEvent.Input -> EventView(timeline, event)
                    is CallEvent.Output -> EventView(timeline, event)
                    is CallEvent.Start -> EventView(event)
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun TimelineItemRow(
    timeline: CallTimeline,
    event: CallEvent,
    selected: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = null,
    onClick: () -> Unit
) {
    ContextMenuArea({
        val result = mutableListOf<ContextMenuItem>()
        when (event) {
            is CallEvent.Start -> {
                result += ContextMenuItem("Copy Authority") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(event.authority), null)
                }
                result += ContextMenuItem("Copy Method Name") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(event.method), null)
                }
                result += ContextMenuItem("Copy Headers") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.header.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Accept -> {
                result += ContextMenuItem("Copy Headers") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.header.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Close -> {
                result += ContextMenuItem("Copy Trails") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.trails.toMap().toJson()
                        ),
                        null
                    )
                }
            }
            is CallEvent.Input -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json), null
                    )
                }
            }
            is CallEvent.Output -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json), null
                    )
                }
            }
        }
        result
    }) {
        SelectionScope(selected) {
            Row(
                modifier = Modifier
                    .height(23.dp)
                    .fillMaxWidth()
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
                    .hoverable(rememberCoroutineScope(), interactionSource),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val icon = when (event) {
                    is CallEvent.Accept -> "icons/reviewAccepted.svg"
                    is CallEvent.Close -> "icons/reviewRejected.svg"
                    is CallEvent.Input -> "icons/showWriteAccess.svg"
                    is CallEvent.Output -> "icons/showReadAccess.svg"
                    is CallEvent.Start -> "icons/connector.svg"
                }
                val text = when (event) {
                    is CallEvent.Accept -> "Server Accepted"
                    is CallEvent.Close -> "Closed"
                    is CallEvent.Input -> "Client Messaging"
                    is CallEvent.Output -> "Server Messaging"
                    is CallEvent.Start -> "Start"
                }

                Icon(icon, modifier = Modifier.padding(7.dp))
                Text(text, maxLines = 1)
            }
        }
    }
}

@Composable
fun EventView(event: CallEvent.Start) {
    Column {
        MetadataView(event.header)
    }
}

@Composable
fun EventView(event: CallEvent.Accept) {
    Column {
        MetadataView(event.header)
    }
}

@Composable
fun EventView(event: CallEvent.Close) {
    Column {
        MetadataView(event.trails)
    }
}

@Composable
fun EventView(call: CallTimeline, event: CallEvent) {
    var resolveFailed by remember { mutableStateOf(false) }

    val serverManager = LocalMainViewModel.current.serverManager
    if (!event.resolved() && serverManager != null) {
        LaunchedEffect(call) {
            if (event.resolved()) return@LaunchedEffect
            val ref = serverManager.reflection(call.start().authority)
            withContext(Dispatchers.IO) {
                ref.collect()
            }
            if (ref.resolved()) {
                call.resolve(ref)
            } else {
                resolveFailed = true
            }
        }
    }

    if (event.resolved()) {
        when (event) {
            is CallEvent.Input -> MessageView(call.reflection(), event.message())
            is CallEvent.Output -> MessageView(call.reflection(), event.message())
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                when {
                    resolveFailed -> {
                        Text("Fail to resolve call")
                    }
                    serverManager != null -> {
                        ProgressBar()
                        Text("Resolving...")
                    }
                    else -> {
                        Text("Need start proxy server to resolve calls")
                    }
                }
            }
        }
    }
}

@Composable
fun MetadataView(metadata: Metadata, modifier: Modifier = Modifier) {
    Column(modifier.background(Color.White).fillMaxSize()) {
        var selectedKey by remember(metadata) { mutableStateOf("") }

        metadata.keys().forEach {
            if (it.endsWith("-bin")) return@forEach
            MetadataItem(
                it,
                metadata[Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)] ?: "",
                selectedKey == it
            ) {
                selectedKey = it
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetadataItem(
    key: String,
    value: String,
    selected: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClick: () -> Unit
) {
    Box(
        Modifier.fillMaxWidth()
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = ListItemHoverIndication,
                onClick = onClick,
                role = null
            ).run {
                if (selected) {
                    background(color = JBTheme.selectionColors.active)
                } else {
                    this
                }
            }
            .hoverable(rememberCoroutineScope(), interactionSource)
    ) {
        ContextMenuArea({
            val result = mutableListOf<ContextMenuItem>()
            result += ContextMenuItem("Copy") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection("$key: $value"), null)
            }
            result += ContextMenuItem("Copy Key") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(key), null)
            }
            result += ContextMenuItem("Copy Value") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(value), null)
            }
            result
        }) {
            SelectionScope(selected) {
                Row(
                    Modifier.height(23.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.padding(horizontal = 7.dp).weight(1f),
                    ) {
                        Label(
                            text = key,
                            modifier = Modifier.align(Alignment.CenterEnd),
                            color = JBTheme.textColors.infoInput,
                            style = JBTheme.typography.defaultBold
                        )
                    }
                    Label(
                        value,
                        Modifier.weight(3f),
                    )
                }
            }
        }
    }
}

internal fun Modifier.rightClickable(onClick: (IntOffset) -> Unit): Modifier {
    return pointerInput(Unit) {
        this.awaitPointerEventScope {
            while (true) {
                val event: PointerEvent = awaitPointerEvent()
                val mouseEvent = event.awtEvent
                if (mouseEvent.button == MouseEvent.BUTTON3 && event.changes.any { it.changedToUp() }) {
                    onClick(IntOffset(mouseEvent.x, mouseEvent.y))
                }
            }
        }
    }
}
