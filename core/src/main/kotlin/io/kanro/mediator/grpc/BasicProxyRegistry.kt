package io.kanro.mediator.grpc

import io.grpc.Channel
import io.grpc.HandlerRegistry
import io.grpc.MethodDescriptor
import io.grpc.ServerMethodDefinition
import java.io.InputStream

abstract class BasicProxyRegistry : HandlerRegistry() {
    private val methods = mutableMapOf<String, ServerMethodDefinition<*, *>>()

    protected abstract fun targetChannel(methodName: String, authority: String?): Channel

    override fun lookupMethod(methodName: String, authority: String?): ServerMethodDefinition<*, *> {
        return methods.getOrPut("$authority:$methodName") {
            ServerMethodDefinition.create(
                MethodDescriptor.newBuilder(ByteArrayMarshaller, ByteArrayMarshaller)
                    .setType(MethodDescriptor.MethodType.UNKNOWN)
                    .setFullMethodName(methodName)
                    .build(),
                ProxyHandler(targetChannel(methodName, authority))
            )
        }
    }

    object ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream {
            return value.inputStream()
        }

        override fun parse(stream: InputStream): ByteArray {
            return stream.readBytes()
        }
    }
}
