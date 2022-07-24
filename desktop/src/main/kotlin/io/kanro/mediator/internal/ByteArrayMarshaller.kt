package io.kanro.mediator.internal

import io.grpc.MethodDescriptor
import java.io.ByteArrayInputStream
import java.io.InputStream

object ByteArrayMarshaller : MethodDescriptor.Marshaller<ByteArray> {
    override fun stream(value: ByteArray): InputStream {
        return ByteArrayInputStream(value)
    }

    override fun parse(stream: InputStream): ByteArray {
        return stream.readBytes()
    }
}
