package io.kanro.pick

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.AwtWindow
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.kanro.compose.jetbrains.JBTheme
import io.kanro.compose.jetbrains.control.Button
import io.kanro.compose.jetbrains.control.ButtonDefaults
import io.kanro.compose.jetbrains.control.JPanel
import io.kanro.compose.jetbrains.control.ListItemHoverIndication
import io.kanro.compose.jetbrains.control.Text
import io.kanro.compose.jetbrains.control.jBorder
import io.kanro.compose.jetbrains.interaction.hoverable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Window
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.system.exitProcess

sealed interface Action

data class MoveAction(val file: ImageFile, val index: Int) : Action

data class JumpAction(val index: Int) : Action

class ImageFile(val file: Path) {
    private var realPath: Path = file

    fun moveTo(dir: Path) {
        Files.createDirectories(dir)
        realPath = dir.resolve(file.name)
        Files.move(file, realPath)
    }

    fun undo() {
        Files.move(realPath, file)
        realPath = file
    }
}

class MainViewModel(val state: LazyListState) {
    val files = mutableStateListOf<ImageFile>()
    val actions = mutableListOf<Action>()
    val selectedFileIndex = mutableStateOf(-1)

    fun load(file: Path) {
        files.clear()
        actions.clear()
        files += file.toFile().listFiles { dir, name ->
            name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(
                ".png"
            )
        }!!.map { ImageFile(it.toPath()) }
        selectedFileIndex.value = if (files.isNotEmpty()) {
            0
        } else {
            -1
        }
    }

    fun moveToUp() {
        moveTo("up")
    }

    fun moveToDown() {
        moveTo("down")
    }

    fun next() {
        jump(selectedFileIndex.value + 1)
    }

    fun previous() {
        jump(selectedFileIndex.value - 1)
    }

    fun undo() {
        if (actions.isNotEmpty()) {
            when (val last = actions.removeLast()) {
                is MoveAction -> {
                    last.file.undo()
                    files.add(last.index, last.file)
                    selectedFileIndex.value = last.index
                }
                is JumpAction -> {
                    selectedFileIndex.value = last.index
                }
            }
        }
    }

    fun jump(newIndex: Int) {
        if (files.isEmpty()) return
        val newIndex = newIndex.coerceIn(0, files.size - 1)
        val index = selectedFileIndex.value
        if (newIndex == index) return
        actions += JumpAction(index)
        selectedFileIndex.value = newIndex
    }

    fun moveTo(dir: String) {
        val index = selectedFileIndex.value
        if (index < 0) return
        val image = files[index]
        files.removeAt(index)
        val newDir = image.file.parent.resolve(dir)
        image.moveTo(newDir)
        actions += MoveAction(image, index)
        if (index == files.size) {
            selectedFileIndex.value -= 1
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    application {
        val listState = rememberLazyListState()
        val vm = remember { MainViewModel(listState) }

        Window(
            title = "PickOrNot",
            state = rememberWindowState(),
            onCloseRequest = {
                exitApplication()
                exitProcess(0)
            },
            onKeyEvent = {
                if (it.type == KeyEventType.KeyDown) {
                    when (it.key) {
                        Key.DirectionRight -> vm.next()
                        Key.DirectionLeft -> vm.previous()
                        Key.DirectionUp -> vm.moveToUp()
                        Key.DirectionDown -> vm.moveToDown()
                        Key.W -> vm.moveTo("classW")
                        Key.A -> vm.moveTo("classA")
                        Key.S -> vm.moveTo("classS")
                        Key.D -> vm.moveTo("classD")
                        Key.One -> vm.moveTo("class1")
                        Key.Two -> vm.moveTo("class2")
                        Key.Three -> vm.moveTo("class3")
                        Key.Four -> vm.moveTo("class4")
                        Key.Z -> if (it.isCtrlPressed) vm.undo()
                    }
                }
                true
            }
        ) {
            CompositionLocalProvider(
                LocalWindow provides window,
            ) {
                JBTheme {
                    JPanel(Modifier.fillMaxSize().jBorder(top = 1.dp, color = JBTheme.panelColors.border)) {

                        Column {
                            Box(Modifier.weight(1f).height(0.dp).fillMaxWidth()) {
                                if (vm.selectedFileIndex.value < 0) {
                                    Text("Open a folder...", Modifier.align(Alignment.Center))
                                } else {
                                    val file = vm.files[vm.selectedFileIndex.value]
                                    AsyncImage(file.file, "", Modifier.align(Alignment.Center))
                                }
                            }

                            LazyRow(
                                Modifier.height(100.dp).fillMaxWidth().background(color = Color.White),
                                state = listState
                            ) {
                                itemsIndexed(vm.files) { index, x ->
                                    FileIcon(x, index == vm.selectedFileIndex.value) {
                                        vm.jump(index)
                                    }
                                }
                            }

                            val adapter = rememberScrollbarAdapter(listState)
                            HorizontalScrollbar(
                                adapter,
                                Modifier.fillMaxWidth().height(10.dp)
                            )

                            val density = LocalDensity.current

                            ComposeScope {
                                val index = vm.selectedFileIndex.value
                                LaunchedEffect(index) {
                                    val width = ((listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset) / density.density).toInt()
                                    val target = index - (width / 200)
                                    if (target < 0) {
                                        listState.animateScrollToItem(0)
                                    } else {
                                        listState.animateScrollToItem(target)
                                    }
                                }
                            }

                            Box(Modifier.height(100.dp).fillMaxWidth().padding(horizontal = 12.dp)) {
                                Row(
                                    modifier = Modifier.align(Alignment.CenterStart)
                                ) {
                                    var chooseFolder by remember { mutableStateOf(false) }
                                    Button({
                                        chooseFolder = true
                                    }) {
                                        Text("Open")

                                        if (chooseFolder) {
                                            FolderDialog {
                                                if (it != null) {
                                                    vm.load(Paths.get(it))
                                                }
                                                chooseFolder = false
                                            }
                                        }
                                    }
                                }

                                Column(
                                    modifier = Modifier.align(Alignment.CenterEnd),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(Modifier.align(Alignment.CenterHorizontally)) {
                                        Button(
                                            {
                                                vm.moveToUp()
                                            },
                                            style = ButtonDefaults.outlineButtonStyle()
                                        ) {
                                            Text("Up")
                                        }
                                    }

                                    Row(
                                        Modifier.align(Alignment.CenterHorizontally),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Button(
                                            {
                                                vm.previous()
                                            },
                                            style = ButtonDefaults.outlineButtonStyle()
                                        ) {
                                            Text("Prev")
                                        }

                                        Button(
                                            {
                                                vm.moveToDown()
                                            },
                                            style = ButtonDefaults.outlineButtonStyle()
                                        ) {
                                            Text("Down")
                                        }

                                        Button(
                                            {
                                                vm.next()
                                            },
                                            style = ButtonDefaults.outlineButtonStyle()
                                        ) {
                                            Text("Next")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeScope(block: @Composable () -> Unit) {
    block()
}

@Composable
fun FileIcon(
    file: ImageFile,
    selected: Boolean,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    role: Role? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .run {
                if (!selected) {
                    background(color = Color.White)
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
            .hoverable(rememberCoroutineScope(), interactionSource)
    ) {
        AsyncImage(file.file, "", Modifier.align(Alignment.Center).size(80.dp))
    }
}

val LocalWindow: ProvidableCompositionLocal<Window> = compositionLocalOf {
    error("CompositionLocal LocalWindow not provided")
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
fun FolderDialog(
    parent: Frame? = null,
    onCloseRequest: (result: String?) -> Unit
) = AwtWindow(
    create = {
        object : FileDialog(parent, "Choose a file", LOAD) {
            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    onCloseRequest(directory)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

@Composable
fun AsyncImage(
    path: Path,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val image: ImageBitmap? by produceState<ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            try {
                org.jetbrains.skija.Image.makeFromEncoded(Files.readAllBytes(path)).asImageBitmap()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        }
    }

    image?.let {
        Image(
            bitmap = it,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}
