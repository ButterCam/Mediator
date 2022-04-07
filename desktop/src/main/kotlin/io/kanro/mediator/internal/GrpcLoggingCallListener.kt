package io.kanro.mediator.internal

import io.grpc.ForwardingServerCallListener
import io.grpc.ServerCall

class GrpcLoggingCallListener<ReqT>(delegate: ServerCall.Listener<ReqT>, private val call: ServerCall<*, *>) :
    ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(delegate) {
    override fun onMessage(message: ReqT) {
        val message = GrpcCallLogger.timelineKey.get().input(message as ByteArray).message
        super.onMessage(message as ReqT)
    }
}
