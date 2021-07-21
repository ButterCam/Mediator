package io.kanro.mediator.internal

import io.grpc.ForwardingServerCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status
import io.kanro.mediator.desktop.model.CallEvent

class GrpcLoggingCall<ReqT, RespT>(delegate: ServerCall<ReqT, RespT>) :
    ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(delegate) {
    override fun sendMessage(message: RespT) {
        GrpcCallLogger.timelineKey.get().emit(
            CallEvent.Output(message as ByteArray)
        )
        super.sendMessage(message)
    }

    override fun sendHeaders(headers: Metadata) {
        GrpcCallLogger.timelineKey.get().emit(
            CallEvent.Accept(headers)
        )
        super.sendHeaders(headers)
    }

    override fun close(status: Status, trailers: Metadata) {
        GrpcCallLogger.timelineKey.get().emit(
            CallEvent.Close(status, trailers)
        )
        super.close(status, trailers)
    }
}
