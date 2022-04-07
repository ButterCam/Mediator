package io.kanro.mediator.internal

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status

class GrpcLoggingCall<ReqT, RespT>(delegate: ServerCall<ReqT, RespT>) :
    ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun sendMessage(message: RespT) {
        val message = GrpcCallLogger.timelineKey.get().output(message as ByteArray).message
        super.sendMessage(message as RespT)
    }

    override fun sendHeaders(headers: Metadata) {
        val accept = GrpcCallLogger.timelineKey.get().accept(headers)
        super.sendHeaders(accept.header)
    }

    override fun close(status: Status, trailers: Metadata) {
        val close = GrpcCallLogger.timelineKey.get().close(status, trailers)
        super.close(status, close.trails)
    }
}
