package io.kanro.mediator.desktop.ui

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.protobuf.Message
import com.bybutter.sisyphus.protobuf.ProtoEnum
import com.bybutter.sisyphus.protobuf.ProtoReflection
import com.bybutter.sisyphus.protobuf.findMapEntryDescriptor
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.primitives.Duration
import com.bybutter.sisyphus.protobuf.primitives.FieldDescriptorProto
import com.bybutter.sisyphus.protobuf.primitives.Timestamp
import com.bybutter.sisyphus.protobuf.primitives.string
import com.bybutter.sisyphus.security.base64
import com.bybutter.sisyphus.string.toCamelCase
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.SelectionScope
import io.kanro.compose.jetbrains.control.JBTreeItem
import io.kanro.compose.jetbrains.control.JBTreeList
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.UUID

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun MessageView(reflection: ProtoReflection, message: Message<*, *>) {
    Box(Modifier.background(JBTheme.panelColors.bgContent).fillMaxSize()) {
        val vState = rememberScrollState()
        JBTreeList(
            Modifier.verticalScroll(vState)
        ) {
            val selectedItem = remember { mutableStateOf("") }

            val fields = reflection {
                val fields = mutableMapOf<FieldDescriptorProto, Any?>()
                for ((field, value) in message) {
                    if (message.has(field.number)) {
                        fields[field] = value
                    }
                }
                fields
            }

            for ((field, value) in fields) {
                FieldView(selectedItem, reflection, field, value)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(vState),
            Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
fun FieldView(
    selectedItem: MutableState<String>,
    reflection: ProtoReflection,
    field: FieldDescriptorProto,
    value: Any?
) {
    if (field.label == FieldDescriptorProto.Label.REPEATED) {
        RepeatedFieldView(Modifier, selectedItem, reflection, field, value)
    } else {
        SimpleFieldView(Modifier, selectedItem, reflection, field, null, value)
    }
}

@Composable
internal fun RepeatedFieldView(
    modifier: Modifier,
    selectedItem: MutableState<String>,
    reflection: ProtoReflection,
    field: FieldDescriptorProto,
    value: Any?,
) {
    var expanded by remember { mutableStateOf(false) }
    val key = remember { UUID.randomUUID().toString() }
    val selected = key == selectedItem.value
    JBTreeItem(
        modifier = modifier.fillMaxWidth(),
        expanded = expanded,
        expanding = {
            expanded = it
        },
        selected = selected,
        onClick = {
            selectedItem.value = key
        },
        content = {
            FieldContextMenu(reflection, field, value) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    SelectionScope(selected) {
                        Label(field.name)
                        when (value) {
                            is List<*> -> {
                                val type =
                                    field.typeName.substringAfterLast('.').takeIf { it.isNotEmpty() }
                                        ?: field.type.toString()
                                            .toCamelCase()
                                Label(": $type[]", color = JBTheme.textColors.infoInput)
                            }
                            is Map<*, *> -> {
                                val entry = reflection.findMapEntryDescriptor(field.typeName)
                                val keyField = entry?.field?.firstOrNull { it.number == 1 }
                                val valueField = entry?.field?.firstOrNull { it.number == 2 }

                                val keyType = keyField?.typeName?.substringAfterLast('.')?.takeIf { it.isNotEmpty() }
                                    ?: keyField?.type?.toString()?.toCamelCase()
                                val valueType =
                                    valueField?.typeName?.substringAfterLast('.')?.takeIf { it.isNotEmpty() }
                                        ?: valueField?.type?.toString()?.toCamelCase()

                                Label(
                                    ": map<${keyType ?: "?"}, ${valueType ?: "?"}>",
                                    color = JBTheme.textColors.infoInput
                                )
                            }
                        }
                    }
                }
            }
        }) {
        when (value) {
            is List<*> -> {
                value.forEachIndexed { index, any ->
                    SimpleFieldView(Modifier, selectedItem, reflection, field, index.toString(), any)
                }
            }
            is Map<*, *> -> {
                val valueField =
                    reflection.findMapEntryDescriptor(field.typeName)?.field?.firstOrNull { it.number == 2 }
                if (valueField != null) {
                    value.forEach { (key, any) ->
                        SimpleFieldView(Modifier, selectedItem, reflection, valueField, key.toString(), any)
                    }
                } else {
                    JBTreeItem(
                        selected = false,
                        onClick = {
                            selectedItem.value = ""
                        }
                    ) {
                        Label("Unknown Map Field")
                    }
                }
            }
        }
    }
}

@Composable
internal fun SimpleFieldView(
    modifier: Modifier,
    selectedItem: MutableState<String>,
    reflection: ProtoReflection,
    field: FieldDescriptorProto,
    prefix: String?,
    value: Any?
) {
    val key = remember { UUID.randomUUID().toString() }
    val selected = key == selectedItem.value
    when (field.type) {
        FieldDescriptorProto.Type.MESSAGE -> {
            MessageFieldView(modifier, selectedItem, reflection, field, prefix, value as Message<*, *>)
        }
        FieldDescriptorProto.Type.ENUM -> JBTreeItem(
            modifier.fillMaxWidth(),
            selected = selected,
            onClick = {
                selectedItem.value = key
            }
        ) {
            FieldContextMenu(reflection, field, value) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Label(prefix ?: field.name)
                    Label(": ${field.typeName.substringAfterLast('.')}", color = JBTheme.textColors.infoInput)
                    Label(" = $value")
                    Label("(${(value as ProtoEnum<*>).number})", color = JBTheme.textColors.infoInput)
                }
            }
        }
        FieldDescriptorProto.Type.BYTES -> JBTreeItem(
            modifier.fillMaxWidth(),
            selected = selected,
            onClick = {
                selectedItem.value = key
            }
        ) {
            FieldContextMenu(reflection, field, value) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Label(prefix ?: field.name)
                    Label(": bytes", color = JBTheme.textColors.infoInput)
                    Label(" = ${(value as ByteArray).base64()}")
                }
            }
        }
        FieldDescriptorProto.Type.STRING -> JBTreeItem(
            modifier.fillMaxWidth(),
            selected = selected,
            onClick = {
                selectedItem.value = key
            }
        ) {
            FieldContextMenu(reflection, field, value) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Label(prefix ?: field.name)
                    Label(": string", color = JBTheme.textColors.infoInput)
                    Label(" = ${value?.toJson()}")
                }
            }
        }
        else -> JBTreeItem(
            modifier.fillMaxWidth(),
            selected = selected,
            onClick = {
                selectedItem.value = key
            }
        ) {
            FieldContextMenu(reflection, field, value) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    Label(prefix ?: field.name)
                    Label(": ${field.type.toString().toCamelCase()}", color = JBTheme.textColors.infoInput)
                    Label(" = $value")
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun FieldContextMenu(
    reflection: ProtoReflection,
    field: FieldDescriptorProto,
    value: Any?,
    content: @Composable () -> Unit
) {
    ContextMenuArea(
        {
            val result = mutableListOf<ContextMenuItem>()
            result += ContextMenuItem("Copy Field Name") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(field.name), null)
            }
            result += ContextMenuItem("Copy Field JSON Name") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(field.jsonName), null)
            }
            val json = reflection {
                value?.toJson() ?: ""
            }
            result += ContextMenuItem("Copy Value as JSON") {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(json), null)
            }
            val stringValue = when (value) {
                is String -> value
                is Timestamp -> value.string()
                is Duration -> value.string()
                else -> null
            }
            if (stringValue != null) {
                result += ContextMenuItem("Copy Value") {
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(stringValue), null)
                }
            }
            result
        },
        content = content
    )
}

@Composable
internal fun MessageFieldView(
    modifier: Modifier,
    selectedItem: MutableState<String>,
    reflection: ProtoReflection,
    field: FieldDescriptorProto,
    prefix: String?,
    message: Message<*, *>
) {
    var expanded by remember { mutableStateOf(false) }
    val key = remember { UUID.randomUUID().toString() }
    val selected = key == selectedItem.value

    JBTreeItem(
        modifier = modifier.fillMaxWidth(),
        expanded = expanded,
        expanding = {
            expanded = it
        },
        selected = selected,
        onClick = {
            selectedItem.value = key
        },
        content = {
            FieldContextMenu(reflection, field, message) {
                Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
                    SelectionScope(selected) {
                        Label(prefix ?: field.name)
                        Label(": ${field.typeName.substringAfterLast('.')}", color = JBTheme.textColors.infoInput)
                        when (message) {
                            is Timestamp -> Label(" = ${message.string()}", color = JBTheme.textColors.infoInput)
                            is Duration -> Label(" = ${message.string()}", color = JBTheme.textColors.infoInput)
                        }
                    }
                }
            }
        }
    ) {
        val fields = reflection {
            val fields = mutableMapOf<FieldDescriptorProto, Any?>()
            for ((field, value) in message) {
                if (message.has(field.number)) {
                    fields[field] = value
                }
            }
            fields
        }
        for ((field, value) in fields) {
            FieldView(selectedItem, reflection, field, value)
        }
    }
}
