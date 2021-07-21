package io.kanro.mediator.desktop.model

import io.kanro.mediator.internal.StatefulProtoReflection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class CallTimeline(val id: String = (counter++).toString()) : BaseObservableObject() {
    private val events: Deque<CallEvent> = ConcurrentLinkedDeque()

    private var reflection: StatefulProtoReflection? = null

    fun reflection(): StatefulProtoReflection {
        return reflection ?: throw IllegalStateException("Unresolved timeline")
    }

    fun events(): Deque<CallEvent> {
        return events
    }

    fun emit(event: CallEvent) {
        events += event
        GlobalScope.launch {
            onChanged(event)
        }
    }

    suspend fun emitSuspend(event: CallEvent) {
        events += event
        onChanged(event)
    }

    fun resolve(ref: StatefulProtoReflection): Boolean {
        var resolved = false
        reflection = ref
        events.forEach {
            resolved = it.resolve(this) || resolved
        }
        if (resolved) {
            GlobalScope.launch {
                onChanged()
            }
        }

        return resolved
    }

    inline fun <reified T> first(): T? {
        events().forEach {
            if (it is T) {
                return it
            }
        }
        return null
    }

    fun start(): CallEvent.Start {
        return first()!!
    }

    fun close(): CallEvent.Close? {
        return first()
    }

    companion object {
        private var counter = 1
    }
}
