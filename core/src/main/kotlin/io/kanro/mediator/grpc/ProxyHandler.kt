package io.kanro.mediator.grpc

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler

class ProxyHandler(private val channel: Channel) : ServerCallHandler<ByteArray, ByteArray> {
    override fun startCall(
        call: ServerCall<ByteArray, ByteArray>,
        headers: Metadata
    ): ServerCall.Listener<ByteArray> {
        val clientCall = channel.newCall(call.methodDescriptor, CallOptions.DEFAULT.withWaitForReady())
        val bridge = CallBridge(call, clientCall)

        clientCall.start(bridge.clientListener(), headers)
        call.request(1)
        clientCall.request(1)

        return bridge.serverListener()
    }
}
