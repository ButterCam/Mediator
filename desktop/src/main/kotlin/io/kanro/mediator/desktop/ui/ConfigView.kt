package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import androidx.compose.ui.window.rememberDialogState
import com.bybutter.sisyphus.string.toTitleCase
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import io.kanro.compose.jetbrains.control.Button
import io.kanro.compose.jetbrains.control.CheckBox
import io.kanro.compose.jetbrains.control.DropdownList
import io.kanro.compose.jetbrains.control.EmbeddedTextField
import io.kanro.compose.jetbrains.control.Icon
import io.kanro.compose.jetbrains.control.JPanelBorder
import io.kanro.compose.jetbrains.control.ListItemHoverIndication
import io.kanro.compose.jetbrains.control.OutlineButton
import io.kanro.compose.jetbrains.control.Text
import io.kanro.compose.jetbrains.control.TextField
import io.kanro.compose.jetbrains.control.jBorder
import io.kanro.mediator.desktop.LocalWindow
import io.kanro.mediator.desktop.model.RequestRule
import io.kanro.mediator.desktop.viewmodel.ConfigViewModel
import io.kanro.mediator.desktop.viewmodel.MetadataEntry
import io.kanro.mediator.desktop.viewmodel.RequestRuleViewModel
import io.kanro.mediator.desktop.viewmodel.ServerRuleViewModel
import java.awt.Cursor

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun ConfigDialog(
    vm: ConfigViewModel,
    onSave: (ConfigViewModel) -> Unit,
    onCloseRequest: () -> Unit,
    state: DialogState = rememberDialogState(size = DpSize(700.dp, 500.dp)),
    visible: Boolean = true,
    title: String = "Mediator Settings",
    icon: Painter? = null,
    undecorated: Boolean = false,
    transparent: Boolean = false,
    resizable: Boolean = false,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: ((KeyEvent) -> Boolean) = { false },
    onKeyEvent: ((KeyEvent) -> Boolean) = { false }
) {
    Dialog(
        onCloseRequest,
        state,
        visible,
        title,
        icon,
        undecorated,
        transparent,
        resizable,
        enabled,
        focusable,
        onPreviewKeyEvent,
        onKeyEvent
    ) {
        CompositionLocalProvider(
            LocalWindow provides this.window
        ) {
            Column(Modifier.background(JBTheme.panelColors.bgDialog)) {
                JPanelBorder(Modifier.height(1.dp).fillMaxWidth())
                Row(Modifier.height(0.dp).weight(1.0f).fillMaxWidth()) {
                    var selectedItem by remember { mutableStateOf(0) }
                    Box(Modifier.background(Color(0xFFE6EBF0)).fillMaxHeight().width(150.dp)) {
                        Column {
                            ConfigItem(
                                selectedItem == 0,
                                {
                                    selectedItem = 0
                                }
                            ) {
                                Text(
                                    "General",
                                    Modifier.padding(horizontal = 12.dp),
                                    style = JBTheme.typography.defaultBold
                                )
                            }
                            ConfigItem(
                                selectedItem == 1,
                                {
                                    selectedItem = 1
                                }
                            ) {
                                Text(
                                    "Server Rule",
                                    Modifier.padding(horizontal = 12.dp),
                                    style = JBTheme.typography.defaultBold
                                )
                            }
                            ConfigItem(
                                selectedItem == 2,
                                {
                                    selectedItem = 2
                                }
                            ) {
                                Text(
                                    "Request Rule",
                                    Modifier.padding(horizontal = 12.dp),
                                    style = JBTheme.typography.defaultBold
                                )
                            }
                        }
                    }
                    JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
                    Box(Modifier.fillMaxSize()) {
                        when (selectedItem) {
                            0 -> GeneralConfigView(vm)
                            1 -> ServerRuleView(vm)
                            2 -> RequestRuleView(vm)
                            else -> {
                            }
                        }
                    }
                }
                Row(Modifier.height(49.dp).jBorder(top = 1.dp, color = JBTheme.panelColors.border)) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                        Row(
                            Modifier.padding(horizontal = 21.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlineButton({
                                onCloseRequest()
                            }) {
                                Text("Cancel")
                            }
                            OutlineButton(
                                {
                                    onSave(vm)
                                    vm.changed.value = false
                                },
                                enabled = vm.changed.value
                            ) {
                                Text("Apply")
                            }
                            Button({
                                onSave(vm)
                                onCloseRequest()
                            }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigItem(
    selected: Boolean,
    onClick: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Box(
        Modifier.fillMaxWidth().height(25.dp)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = null
            ).run {
                if (selected) {
                    background(color = JBTheme.selectionColors.active)
                } else {
                    this
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        SelectionScope(selected, block = content)
    }
}

@Composable
fun GeneralConfigView(vm: ConfigViewModel) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("General", style = JBTheme.typography.defaultBold)

        Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Proxy port:", modifier = Modifier.width(100.dp))
            var error by remember { mutableStateOf(false) }
            TextField(
                vm.proxyPort.value,
                onValueChange = {
                    vm.changed.value = true
                    if (it.isEmpty()) {
                        error = true
                        vm.proxyPort.value = it
                    }

                    it.toIntOrNull()?.let {
                        if (it < 0) {
                            vm.proxyPort.value = "0"
                        } else if (it > 65535) {
                            vm.proxyPort.value = "65535"
                        } else {
                            vm.proxyPort.value = it.toString()
                        }
                        error = false
                    } ?: kotlin.run {
                        vm.proxyPort.value = vm.proxyPort.value
                    }
                },
                isError = error, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Grpc Server port:", modifier = Modifier.width(100.dp))
            var error by remember { mutableStateOf(false) }
            TextField(
                vm.grpcPort.value,
                {
                    vm.changed.value = true
                    if (it.isEmpty()) {
                        error = true
                        vm.grpcPort.value = it
                    }

                    it.toIntOrNull()?.let {
                        if (it < 0) {
                            vm.grpcPort.value = "0"
                        } else if (it > 65535) {
                            vm.grpcPort.value = "65535"
                        } else {
                            vm.grpcPort.value = it.toString()
                        }
                        error = false
                    } ?: kotlin.run {
                        vm.grpcPort.value = vm.grpcPort.value
                    }
                },
                isError = error, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}

@Composable
fun ServerRuleView(vm: ConfigViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        var selectedRule by remember { mutableStateOf<ServerRuleViewModel?>(null) }

        ListToolBar(
            orientation = Orientation.Horizontal,
            list = vm.serverRules,
            selectedItem = selectedRule,
            onCreate = {
                vm.serverRules += ServerRuleViewModel()
                vm.changed.value = true
            },
            onRemove = {
                vm.serverRules.remove(it)
                selectedRule = null
                vm.changed.value = true
            },
            onMoveUp = onMoveUp@{
                val rule = it ?: return@onMoveUp
                val index = vm.serverRules.indexOf(rule)
                if (index <= 0) return@onMoveUp
                vm.serverRules.remove(rule)
                vm.serverRules.add(index - 1, rule)
                vm.changed.value = true
            },
            onMoveDown = onMoveDown@{
                val rule = it ?: return@onMoveDown
                val index = vm.serverRules.indexOf(rule)
                if (index < 0) return@onMoveDown
                vm.serverRules.remove(rule)
                vm.serverRules.add(index + 1, rule)
                vm.changed.value = true
            }
        )
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(
                Modifier.width(150.dp).fillMaxHeight().background(JBTheme.panelColors.bgContent)
                    .jBorder(1.dp, JBTheme.panelColors.border)
            ) {
                items(vm.serverRules) { x ->
                    RuleRow(
                        x.name.value, x.enabled.value, selectedRule == x,
                        {
                            selectedRule = x
                        }
                    )
                }
            }
            val rule = selectedRule
            if (rule != null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckBox(
                            rule.enabled.value,
                            {
                                rule.enabled.value = it
                                vm.changed.value = true
                            }
                        )
                        Text("Enable server rule")
                    }
                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Title:", Modifier.width(80.dp))
                        TextField(
                            rule.name.value,
                            onValueChange = {
                                rule.name.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp),
                            singleLine = true
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Host pattern:", Modifier.width(80.dp))
                        TextField(
                            rule.regex.value,
                            onValueChange = {
                                rule.regex.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp), isError = !rule.regex.value.isValidRegex(),
                            singleLine = true
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Rewrite:", Modifier.width(80.dp))
                        TextField(
                            rule.replace.value,
                            onValueChange = {
                                rule.replace.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp),
                            singleLine = true,
                            enabled = rule.replaceEnabled.value
                        )
                        Spacer(Modifier.width(8.dp))
                        CheckBox(
                            rule.replaceEnabled.value,
                            onCheckedChange = {
                                rule.replaceEnabled.value = it
                            }
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Reflection api metadata:")
                        MetadataView(vm, rule.reflectionMetadata, Modifier.height(0.dp).weight(1.0f).fillMaxWidth())
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select one server rule to configure")
                }
            }
        }
    }
}

@Composable
fun RequestRuleView(vm: ConfigViewModel) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp)) {
        var selectedRule by remember { mutableStateOf<RequestRuleViewModel?>(null) }

        ListToolBar(
            orientation = Orientation.Horizontal,
            list = vm.requestRules,
            selectedItem = selectedRule,
            onCreate = {
                vm.requestRules += RequestRuleViewModel()
                vm.changed.value = true
            },
            onRemove = {
                vm.requestRules.remove(it)
                selectedRule = null
                vm.changed.value = true
            },
            onMoveUp = onMoveUp@{
                val rule = it ?: return@onMoveUp
                val index = vm.requestRules.indexOf(rule)
                if (index <= 0) return@onMoveUp
                vm.requestRules.remove(rule)
                vm.requestRules.add(index - 1, rule)
                vm.changed.value = true
            },
            onMoveDown = onMoveDown@{
                val rule = it ?: return@onMoveDown
                val index = vm.requestRules.indexOf(rule)
                if (index < 0) return@onMoveDown
                vm.requestRules.remove(rule)
                vm.requestRules.add(index + 1, rule)
                vm.changed.value = true
            }
        )
        Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            LazyColumn(
                Modifier.width(150.dp).fillMaxHeight().background(JBTheme.panelColors.bgContent)
                    .jBorder(1.dp, JBTheme.panelColors.border)
            ) {
                items(vm.requestRules) { x ->
                    RuleRow(
                        x.name.value, x.enabled.value, selectedRule == x,
                        {
                            selectedRule = x
                        }
                    )
                }
            }
            val rule = selectedRule
            if (rule != null) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CheckBox(
                            rule.enabled.value,
                            {
                                rule.enabled.value = it
                                vm.changed.value = true
                            }
                        )
                        Text("Enable request rule")
                    }
                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Title:", Modifier.width(80.dp))
                        TextField(
                            rule.name.value,
                            onValueChange = {
                                rule.name.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp),
                            singleLine = true
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Method:", Modifier.width(80.dp))
                        TextField(
                            rule.method.value,
                            onValueChange = {
                                rule.method.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp), isError = !rule.method.value.isValidRegex(),
                            singleLine = true
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Type:", Modifier.width(80.dp))
                        DropdownList(
                            RequestRule.Type.values().toList(),
                            rule.type.value,
                            onValueChange = {
                                rule.type.value = it
                                vm.changed.value = true
                            },
                            valueRender = {
                                it.toString().toTitleCase()
                            }
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Operation:", Modifier.width(80.dp))
                        DropdownList(
                            RequestRule.Operation.values().toList(),
                            rule.op.value,
                            onValueChange = {
                                rule.op.value = it
                                vm.changed.value = true
                            },
                            valueRender = {
                                it.toString().toTitleCase()
                            }
                        )
                    }

                    Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (rule.op.value) {
                            RequestRule.Operation.MOVE,
                            RequestRule.Operation.COPY -> {
                                Text("From:", Modifier.width(80.dp))
                            }
                            else -> {
                                Text("Path:", Modifier.width(80.dp))
                            }
                        }
                        TextField(
                            rule.path.value,
                            onValueChange = {
                                rule.path.value = it.trim()
                                vm.changed.value = true
                            },
                            Modifier.width(200.dp),
                            singleLine = true
                        )
                    }

                    when (rule.op.value) {
                        RequestRule.Operation.REMOVE -> {}
                        RequestRule.Operation.TEST,
                        RequestRule.Operation.ADD,
                        RequestRule.Operation.REPLACE -> {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Value:", Modifier.width(80.dp))
                                TextField(
                                    rule.value.value,
                                    onValueChange = {
                                        rule.value.value = it.trim()
                                        vm.changed.value = true
                                    },
                                    Modifier.fillMaxSize(),
                                    singleLine = false,
                                    maxLines = Int.MAX_VALUE
                                )
                            }
                        }
                        RequestRule.Operation.COPY,
                        RequestRule.Operation.MOVE -> {
                            Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("To:", Modifier.width(80.dp))
                                TextField(
                                    rule.value.value,
                                    onValueChange = {
                                        rule.value.value = it.trim()
                                        vm.changed.value = true
                                    },
                                    Modifier.width(200.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select one request rule to configure")
                }
            }
        }
    }
}

@Composable
fun RuleRow(
    name: String,
    enabled: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    SelectionScope(selected) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ListItemHoverIndication,
                    onClick = onSelect,
                    role = null
                ).run {
                    if (selected) {
                        background(color = JBTheme.selectionColors.active)
                    } else {
                        this
                    }
                }
                .hoverable(interactionSource)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.size(16.dp)) {
                if (enabled) {
                    Icon("icons/toolbarPassed.svg")
                }
            }
            Text(name)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetadataView(
    configVm: ConfigViewModel,
    metadata: SnapshotStateList<MetadataEntry>,
    modifier: Modifier = Modifier,
) {
    Row(modifier.jBorder(1.dp, JBTheme.panelColors.border)) {
        var selectedEntry by remember { mutableStateOf<MetadataEntry?>(null) }
        var keyWidth by remember { mutableStateOf(150.dp) }
        LazyColumn(Modifier.width(0.dp).weight(1.0f).fillMaxHeight().background(JBTheme.panelColors.bgContent)) {
            stickyHeader {
                MetadataRowHeader(keyWidth) {
                    keyWidth += it.dp
                }
            }
            items(metadata) { x ->
                MetadataRow(
                    keyWidth, configVm, x, selectedEntry == x,
                    {
                        selectedEntry = x
                    }
                )
            }
        }
        JPanelBorder(Modifier.width(1.dp).fillMaxHeight())
        ListToolBar(
            list = metadata,
            selectedItem = selectedEntry,
            orientation = Orientation.Vertical,
            onCreate = {
                metadata += MetadataEntry("(new)")
                configVm.changed.value = true
            },
            onRemove = {
                metadata.remove(selectedEntry)
                selectedEntry = null
                configVm.changed.value = true
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MetadataRowHeader(
    keyWidth: Dp,
    resizing: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .height(28.dp)
            .fillMaxWidth()
            .background(JBTheme.panelColors.bgContent),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val fromState = rememberDraggableState { delta ->
            resizing(delta)
        }

        Box(Modifier.width(keyWidth)) {
            Text("Key", Modifier.padding(start = 7.dp))
        }
        JPanelBorder(
            Modifier.width(1.dp).fillMaxHeight()
                .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = fromState
                )
        )
        Box(Modifier.width(0.dp).weight(1.0f)) {
            Text("Value", Modifier.padding(start = 7.dp))
        }
    }
}

@Composable
fun MetadataRow(
    keyWidth: Dp,
    vm: ConfigViewModel,
    entry: MetadataEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    SelectionScope(selected) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .fillMaxWidth()
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = ListItemHoverIndication,
                    onClick = onSelect,
                    role = null
                ).run {
                    if (selected) {
                        background(color = JBTheme.selectionColors.active)
                    } else {
                        this
                    }
                }
                .hoverable(interactionSource),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmbeddedTextField(
                entry.key.value,
                {
                    entry.key.value = it.trim()
                    vm.changed.value = true
                },
                Modifier.width(keyWidth).onFocusEvent {
                    if (it.hasFocus) {
                        onSelect()
                    }
                },
                singleLine = true
            )
            Spacer(Modifier.width(1.dp))
            EmbeddedTextField(
                entry.value.value,
                {
                    entry.value.value = it.trim()
                    vm.changed.value = true
                },
                Modifier.width(0.dp).weight(1.0f).onFocusEvent {
                    if (it.hasFocus) {
                        onSelect()
                    }
                },
                singleLine = true
            )
        }
    }
}

fun String.isValidRegex(): Boolean {
    return try {
        toRegex()
        true
    } catch (e: Exception) {
        false
    }
}
