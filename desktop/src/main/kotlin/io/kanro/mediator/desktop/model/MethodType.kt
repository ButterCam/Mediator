package io.kanro.mediator.desktop.model

import io.grpc.MethodDescriptor

enum class MethodType {
    UNKNOWN, UNARY, CLIENT_STREAMING, SERVER_STREAMING, BIDI_STREAMING;

    fun toGrpcType(): MethodDescriptor.MethodType {
        return when (this) {
            UNARY -> MethodDescriptor.MethodType.UNARY
            CLIENT_STREAMING -> MethodDescriptor.MethodType.CLIENT_STREAMING
            SERVER_STREAMING -> MethodDescriptor.MethodType.SERVER_STREAMING
            BIDI_STREAMING -> MethodDescriptor.MethodType.BIDI_STREAMING
            UNKNOWN -> MethodDescriptor.MethodType.UNKNOWN
        }
    }

    companion object {
        operator fun invoke(methodType: MethodDescriptor.MethodType): MethodType {
            return when (methodType) {
                MethodDescriptor.MethodType.UNARY -> UNARY
                MethodDescriptor.MethodType.CLIENT_STREAMING -> CLIENT_STREAMING
                MethodDescriptor.MethodType.SERVER_STREAMING -> SERVER_STREAMING
                MethodDescriptor.MethodType.BIDI_STREAMING -> BIDI_STREAMING
                MethodDescriptor.MethodType.UNKNOWN -> UNKNOWN
            }
        }
    }
}
