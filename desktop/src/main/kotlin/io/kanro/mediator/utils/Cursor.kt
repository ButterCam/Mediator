package io.kanro.mediator.utils

import androidx.compose.desktop.AppManager
import java.awt.Cursor

fun currentCursor(cursor: Int) {
    val jFrame = AppManager.focusedWindow?.window
    jFrame?.let {
        it.cursor = Cursor.getPredefinedCursor(cursor)
    }
}

fun currentCursor(cursor: Cursor) {
    val jFrame = AppManager.focusedWindow?.window
    jFrame?.let {
        it.cursor = cursor
    }
}
