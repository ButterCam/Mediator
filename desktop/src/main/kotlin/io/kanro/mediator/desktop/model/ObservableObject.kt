package io.kanro.mediator.desktop.model

import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.collect

interface ChangedInteraction : Interaction {
    object Changed : ChangedInteraction
}

interface ObservableObject {
    val interactionSource: InteractionSource
}

abstract class BaseObservableObject : ObservableObject {
    private val source = MutableInteractionSource()

    protected suspend fun onChanged(interaction: ChangedInteraction = ChangedInteraction.Changed) {
        source.emit(interaction)
    }

    override val interactionSource: InteractionSource get() = source
}

@Composable
fun <T : ObservableObject> T.asState(): State<T> {
    val state = remember(this) { mutableStateOf(this, neverEqualPolicy()) }
    LaunchedEffect(this) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is ChangedInteraction -> state.value = this@asState
            }
        }
    }
    return state
}
