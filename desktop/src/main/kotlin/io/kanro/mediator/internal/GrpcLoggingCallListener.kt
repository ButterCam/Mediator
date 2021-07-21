package io.kanro.mediator.internal

import io.grpc.ForwardingServerCallListener
import io.grpc.ServerCall
import io.kanro.mediator.desktop.model.CallEvent

class GrpcLoggingCallListener<ReqT>(delegate: ServerCall.Listener<ReqT>, private val call: ServerCall<*, *>) :
    ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
    override fun onMessage(message: ReqT) {
        GrpcCallLogger.timelineKey.get().emit(
            CallEvent.Input(message as ByteArray)
        )
        super.onMessage(message)
    }
}
