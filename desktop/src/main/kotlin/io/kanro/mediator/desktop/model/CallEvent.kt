package io.kanro.mediator.desktop.model

import com.bybutter.sisyphus.protobuf.Message
import com.bybutter.sisyphus.protobuf.findMessageSupport
import com.bybutter.sisyphus.protobuf.findServiceSupport
import com.bybutter.sisyphus.protobuf.invoke
import com.bybutter.sisyphus.protobuf.primitives.MethodDescriptorProto
import com.bybutter.sisyphus.protobuf.primitives.Timestamp
import com.bybutter.sisyphus.protobuf.primitives.now
import io.grpc.Metadata
import io.grpc.Status

sealed interface CallEvent : ChangedInteraction {
    class Start(
        val authority: String,
        val resolvedAuthority: String,
        val method: String,
        val methodType: MethodType,
        val header: Metadata
    ) : CallEvent {
        private val timestamp = Timestamp.now()

        private var methodDescriptor: MethodDescriptorProto? = null

        fun descriptor(): MethodDescriptorProto {
            return methodDescriptor ?: throw IllegalStateException("Unresolved")
        }

        override fun resolved(): Boolean {
            return methodDescriptor != null
        }

        override fun timestamp() = timestamp

        override fun resolve(timeline: CallTimeline): Boolean {
            if (methodDescriptor == null) {
                val service =
                    timeline.reflection().findServiceSupport(".${timeline.start().method.substringBeforeLast('/')}")
                methodDescriptor =
                    service.descriptor.method.first { it.name == timeline.start().method.substringAfterLast('/') }
                return true
            }
            return false
        }
    }

    class Accept(
        val header: Metadata,
    ) : CallEvent {
        private val timestamp = Timestamp.now()

        override fun timestamp() = timestamp

        override fun resolve(timeline: CallTimeline): Boolean {
            return false
        }

        override fun resolved(): Boolean {
            return true
        }
    }

    class Input(
        val message: ByteArray,
        private var parsedMessage: Message<*, *>? = null
    ) : CallEvent {
        private val timestamp = Timestamp.now()

        override fun timestamp() = timestamp

        override fun resolved(): Boolean {
            return parsedMessage != null
        }

        fun message(): Message<*, *> {
            return parsedMessage ?: throw IllegalStateException("Unresolved")
        }

        fun messageOrNull(): Message<*, *>? {
            return parsedMessage
        }

        override fun resolve(timeline: CallTimeline): Boolean {
            if (parsedMessage == null) {
                timeline.reflection().invoke {
                    val input = timeline.reflection().findMessageSupport(timeline.start().descriptor().inputType)
                    parsedMessage = input.parse(message)
                }
                return true
            }
            return false
        }
    }

    class Output(
        val message: ByteArray,
        private var parsedMessage: Message<*, *>? = null
    ) : CallEvent {
        private val timestamp = Timestamp.now()

        fun message(): Message<*, *> {
            return parsedMessage ?: throw IllegalStateException("Unresolved")
        }

        fun messageOrNull(): Message<*, *>? {
            return parsedMessage
        }

        override fun resolved(): Boolean {
            return parsedMessage != null
        }

        override fun timestamp() = timestamp

        override fun resolve(timeline: CallTimeline): Boolean {
            if (parsedMessage == null) {
                timeline.reflection().invoke {
                    val output = timeline.reflection().findMessageSupport(timeline.start().descriptor().outputType)
                    parsedMessage = output.parse(message)
                }
                return true
            }
            return false
        }
    }

    class Close(
        val status: Status,
        val trails: Metadata,
    ) : CallEvent {
        private val timestamp = Timestamp.now()

        override fun timestamp() = timestamp

        override fun resolve(timeline: CallTimeline): Boolean {
            return false
        }

        override fun resolved(): Boolean {
            return true
        }
    }

    fun timestamp(): Timestamp

    fun resolve(timeline: CallTimeline): Boolean

    fun resolved(): Boolean
}
