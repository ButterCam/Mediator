package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bybutter.sisyphus.coroutine.io
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.protobuf.ProtoReflection
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.primitives.FieldDescriptorProto
import com.bybutter.sisyphus.protobuf.primitives.string
import com.bybutter.sisyphus.rpc.Code
import com.bybutter.sisyphus.rpc.Status
import com.bybutter.sisyphus.string.toUpperSpaceCase
import io.grpc.Metadata
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.control.*
import io.kanro.compose.jetbrains.control.ContextMenuArea
import io.kanro.mediator.desktop.model.CallEvent
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.asState
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.utils.toMap
import java.awt.Cursor
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CallView(call: CallTimeline?) {
    var selectedTab by remember { mutableStateOf(0) }
    var height by remember { mutableStateOf(200.dp) }

    Box(Modifier.fillMaxWidth()) {
        JPanelBorder(Modifier.fillMaxWidth().height(1.dp).align(Alignment.BottomStart))

        val density = LocalDensity.current

        Box(
            Modifier.matchParentSize().pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        with(density) {
                            height -= delta.toDp()
                        }
                    },
                ),
        ) {}
        Row(Modifier.fillMaxWidth().align(Alignment.CenterStart)) {
            Tab(
                selectedTab == 0,
                {
                    selectedTab = 0
                },
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon("icons/ppLib.svg")
                    Text("Statistics", modifier = Modifier.offset(y = (-1).dp))
                }
            }
            Tab(
                selectedTab == 1,
                {
                    selectedTab = 1
                },
            ) {
                Row(
                    modifier = Modifier.padding(7.dp, 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
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
    Column(Modifier.background(JBTheme.panelColors.bgContent).fillMaxSize()) {
        var selectedKey by remember(call) { mutableStateOf(-1) }

        MetadataItem("ID", call.id, selectedKey == 0) {
            selectedKey = 0
        }
        val start = call.start()
        if (start != null) {
            MetadataItem("Host", start.resolvedAuthority, selectedKey == 1) {
                selectedKey = 1
            }
            MetadataItem("Authority", start.authority, selectedKey == 2) {
                selectedKey = 2
            }
            MetadataItem("Method", start.method, selectedKey == 3) {
                selectedKey = 3
            }
            MetadataItem("Start time", start.timestamp().string(), selectedKey == 4) {
                selectedKey = 4
            }
            MetadataItem("Frontend Channel", start.frontend, selectedKey == 5) {
                selectedKey = 5
            }
            MetadataItem("Backend Channel", start.backend, selectedKey == 6) {
                selectedKey = 6
            }
        }
        val close = call.close()
        if (close != null) {
            MetadataItem("End time", close.timestamp().string(), selectedKey == 7) {
                selectedKey = 7
            }
        }
        val transparent = call.first<CallEvent.Transparent>()
        if (transparent != null) {
            MetadataItem("Authority", transparent.authority, selectedKey == 8) {
                selectedKey = 8
            }
            MetadataItem("Start time", transparent.timestamp().string(), selectedKey == 9) {
                selectedKey = 9
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TimelineView(call: CallTimeline) {
    var selectedEvent by remember(call) { mutableStateOf<CallEvent?>(null) }
    var width by remember { mutableStateOf(200.dp) }

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

        JPanelBorder(
            Modifier.width(1.dp).fillMaxHeight()
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        width += delta.dp
                    },
                ),
        )

        Box(Modifier.fillMaxSize()) {
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
                    else -> {}
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
    onClick: () -> Unit,
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
                            event.header.toMap().toJson(),
                        ),
                        null,
                    )
                }
            }

            is CallEvent.Accept -> {
                result += ContextMenuItem("Copy Headers") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.header.toMap().toJson(),
                        ),
                        null,
                    )
                }
            }

            is CallEvent.Close -> {
                result += ContextMenuItem("Copy Trails") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(
                            event.trails.toMap().toJson(),
                        ),
                        null,
                    )
                }
            }

            is CallEvent.Input -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json),
                        null,
                    )
                }
            }

            is CallEvent.Output -> {
                val json = timeline.reflection().invoke {
                    event.message().toJson()
                }
                result += ContextMenuItem("Copy Message") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection(json),
                        null,
                    )
                }
            }

            else -> {}
        }
        result
    }) {
        SelectionScope(selected) {
            Row(
                modifier = Modifier.height(23.dp).fillMaxWidth().selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ListItemHoverIndication,
                    onClick = onClick,
                    role = role,
                ).run {
                    if (selected) {
                        background(color = JBTheme.selectionColors.active)
                    } else {
                        this
                    }
                }.hoverable(interactionSource),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val icon = when (event) {
                    is CallEvent.Transparent -> "icons/testUnknown.svg"
                    is CallEvent.Accept -> "icons/reviewAccepted.svg"
                    is CallEvent.Close -> "icons/reviewRejected.svg"
                    is CallEvent.Input -> "icons/showWriteAccess.svg"
                    is CallEvent.Output -> "icons/showReadAccess.svg"
                    is CallEvent.Start -> "icons/connector.svg"
                }
                val text = when (event) {
                    is CallEvent.Transparent -> "Transparent"
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
    var resolveFailed: String? by remember { mutableStateOf(null) }

    val serverManager = MainViewModel.serverManager
    if (!event.resolved() && serverManager != null) {
        LaunchedEffect(call) {
            io {
                try {
                    if (event.resolved()) return@io
                    if (!call.resolve()) {
                        resolveFailed = "Fail to resolve call"
                    }
                } catch (e: Exception) {
                    resolveFailed = "Fail to resolve call: ${e.message}"
                }
            }
        }
    }

    if (event.resolved()) {
        when (event) {
            is CallEvent.Input -> MessageView(call.reflection(), event.message())
            is CallEvent.Output -> MessageView(call.reflection(), event.message())
            else -> {}
        }
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column {
                val message = resolveFailed
                when {
                    message != null -> {
                        Text(message)
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
    Box(Modifier.background(JBTheme.panelColors.bgContent).fillMaxSize()) {
        val vState = rememberScrollState()
        JBTreeList(modifier.verticalScroll(vState)) {
            val selectedKey = remember(metadata) { mutableStateOf("") }

            metadata.keys().forEach {
                val key = it.lowercase()
                if (key.endsWith("-bin")) {
                    val value = metadata[Metadata.Key.of(it, Metadata.BINARY_BYTE_MARSHALLER)] ?: byteArrayOf()
                    MessageFieldView(
                        Modifier,
                        selectedKey,
                        ProtoReflection.current(),
                        FieldDescriptorProto {
                            this.type = FieldDescriptorProto.Type.MESSAGE
                            this.name = "status"
                            this.typeName = Status.name
                        },
                        it,
                        Status.parse(value),
                    )
                } else {
                    val value = metadata[Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)] ?: ""
                    MetadataItem(
                        it,
                        value,
                        selectedKey.value == it,
                    ) {
                        selectedKey.value = it
                    }
                }
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
    onClick: () -> Unit,
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
        JBTreeItem(modifier = Modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
            Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Label(key)
                Label(" = ", color = JBTheme.textColors.infoInput)
                Label(value)
                if (key.lowercase() == "grpc-status") {
                    val code = Code.fromNumber(value.toInt())
                    if (code != null) {
                        Label(" (${code.name.toUpperSpaceCase()})", color = JBTheme.textColors.infoInput)
                    }
                }
            }
        }
    }
}
