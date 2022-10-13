package io.kanro.mediator.desktop.model

import io.grpc.Metadata
import io.grpc.Status
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.internal.MediatorProtoReflection
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class CallTimeline(val id: String = (counter++).toString()) : BaseObservableObject() {
    private val events: Deque<CallEvent> = ConcurrentLinkedDeque()

    private var reflection: MediatorProtoReflection? = null

    fun reflection(): MediatorProtoReflection {
        return reflection ?: throw IllegalStateException("Unresolved timeline")
    }

    fun events(): Deque<CallEvent> {
        return events
    }

    private fun <T : CallEvent> emit(event: T): T {
        events += event
        GlobalScope.launch {
            onChanged(event)
        }
        return event
    }

    fun transparent(
        authority: String
    ): CallEvent.Transparent {
        return emit(
            CallEvent.Transparent(
                authority
            )
        )
    }

    fun start(
        ssl: Boolean,
        authority: String,
        resolvedAuthority: String,
        method: String,
        methodType: MethodType,
        header: Metadata
    ): CallEvent.Start {
        return emit(
            CallEvent.Start(
                ssl,
                authority,
                resolvedAuthority,
                method,
                methodType,
                header
            )
        )
    }

    fun input(data: ByteArray): CallEvent.Input {
        return emit(CallEvent.Input(data))
    }

    fun accept(header: Metadata): CallEvent.Accept {
        return emit(CallEvent.Accept(header))
    }

    fun output(data: ByteArray): CallEvent.Output {
        return emit(CallEvent.Output(data))
    }

    fun close(status: Status, header: Metadata): CallEvent.Close {
        return emit(CallEvent.Close(status, header))
    }

    fun resolve(): Boolean {
        val start = start() ?: return false
        if (reflection == null) {
            reflection = MainViewModel.serverManager
                ?.reflection(start.authority, start.ssl)
        }
        if (reflection == null) return false
        events.forEach {
            if (it.resolved()) return@forEach
            it.resolve(this)
        }
        GlobalScope.launch {
            onChanged()
        }
        return true
    }

    inline fun <reified T> first(): T? {
        events().forEach {
            if (it is T) {
                return it
            }
        }
        return null
    }

    fun start(): CallEvent.Start? {
        return first()
    }

    fun close(): CallEvent.Close? {
        return first()
    }

    companion object {
        private var counter = 1
    }
}
