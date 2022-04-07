package io.kanro.mediator.desktop.model

import com.bybutter.sisyphus.jackson.Json
import com.bybutter.sisyphus.jackson.parseJson
import com.bybutter.sisyphus.jackson.toJson
import com.bybutter.sisyphus.protobuf.Message
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.jackson.JacksonReader
import com.fasterxml.jackson.databind.JsonNode
import io.grpc.Metadata
import io.grpc.Status
import io.kanro.mediator.desktop.viewmodel.MainViewModel
import io.kanro.mediator.internal.StatefulProtoReflection
import io.kanro.mediator.utils.applyMap
import io.kanro.mediator.utils.toMap
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Deque
import java.util.concurrent.ConcurrentLinkedDeque

class CallTimeline(val id: String = (counter++).toString()) : BaseObservableObject() {
    private val events: Deque<CallEvent> = ConcurrentLinkedDeque()

    private var reflection: StatefulProtoReflection? = null

    private var rules = listOf<RequestRule>()

    fun reflection(): StatefulProtoReflection {
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

    fun start(
        authority: String,
        method: String,
        methodType: MethodType,
        header: Metadata,
        rules: List<RequestRule>
    ): CallEvent.Start {
        this.rules = rules
        val applyingRules = rules.filter { it.type == RequestRule.Type.REQUEST_METADATA }
        return emit(CallEvent.Start(authority, method, methodType, patchMetadata(header, applyingRules)))
    }

    fun input(data: ByteArray): CallEvent.Input {
        val applyingRules = rules.filter { it.type == RequestRule.Type.INPUT }
        var input = CallEvent.Input(data)

        if (applyingRules.isNotEmpty()) {
            if (!resolve()) {
                emit(input)
                return input
            }
            input.resolve(this)
            val resolvedMessage = input.messageOrNull() ?: kotlin.run {
                emit(input)
                return input
            }
            val patchedMessage = patchMessage(resolvedMessage, applyingRules)
            input = CallEvent.Input(patchedMessage.toProto(), patchedMessage)
        }
        return emit(input)
    }

    fun accept(header: Metadata): CallEvent.Accept {
        val applyingRules = rules.filter { it.type == RequestRule.Type.RESPONSE_METADATA }
        return emit(CallEvent.Accept(patchMetadata(header, applyingRules)))
    }

    fun output(data: ByteArray): CallEvent.Output {
        val applyingRules = rules.filter { it.type == RequestRule.Type.OUTPUT }
        var output = CallEvent.Output(data)

        if (applyingRules.isNotEmpty()) {
            if (!resolve()) {
                emit(output)
                return output
            }
            output.resolve(this)
            val resolvedMessage = output.messageOrNull() ?: kotlin.run {
                emit(output)
                return output
            }
            val patchedMessage = patchMessage(resolvedMessage, applyingRules)
            output = CallEvent.Output(patchedMessage.toProto(), patchedMessage)
        }
        return emit(output)
    }

    fun close(status: Status, header: Metadata): CallEvent.Close {
        val applyingRules = rules.filter { it.type == RequestRule.Type.TRAILER }
        return emit(CallEvent.Close(status, patchMetadata(header, applyingRules)))
    }

    private fun patchMetadata(metadata: Metadata, rules: List<RequestRule>): Metadata {
        if (rules.isEmpty()) return metadata
        val node = rules.fold(metadata.toMap().toJson().parseJson<JsonNode>()) { acc, rule ->
            rule.toPatch().apply(acc)
        }
        val map = node.toJson().parseJson<Map<String, String>>()
        return Metadata().applyMap(map)
    }

    private fun patchMessage(message: Message<*, *>, rules: List<RequestRule>): Message<*, *> {
        if (rules.isEmpty()) return message
        return reflection!!.invoke {
            val node = rules.fold(message.toJson().parseJson<JsonNode>()) { acc, rule ->
                rule.toPatch().apply(acc)
            }
            message.support().invoke {
                val reader = JacksonReader(Json.mapper.factory.createParser(node.toJson()))
                reader.next()
                readFrom(reader)
            }
        }
    }

    fun resolve(): Boolean {
        if (reflection == null) {
            reflection = MainViewModel.serverManager?.reflection(start().authority)?.collect()
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
