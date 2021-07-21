package io.kanro.mediator.grpc

import io.grpc.ClientCall
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.Status

class CallBridge(
    val serverCall: ServerCall<ByteArray, ByteArray>,
    val clientCall: ClientCall<ByteArray, ByteArray>
) {
    private val clientListener = object : ClientCall.Listener<ByteArray>() {
        override fun onMessage(message: ByteArray?) {
            synchronized(this@CallBridge) {
                serverCall.sendMessage(message)
                clientCall.request(1)
            }
        }

        override fun onHeaders(headers: Metadata?) {
            synchronized(this@CallBridge) {
                serverCall.sendHeaders(headers)
            }
        }

        override fun onClose(status: Status?, trailers: Metadata?) {
            synchronized(this@CallBridge) {
                serverCall.close(status, trailers)
            }
        }
    }

    private val serverListener = object : ServerCall.Listener<ByteArray>() {
        override fun onHalfClose() {
            synchronized(this@CallBridge) {
                clientCall.halfClose()
            }
        }

        override fun onCancel() {
            synchronized(this@CallBridge) {
                clientCall.cancel("Server cancelled", null)
            }
        }

        override fun onMessage(message: ByteArray?) {
            synchronized(this@CallBridge) {
                clientCall.sendMessage(message)
                serverCall.request(1)
            }
        }
    }

    fun clientListener(): ClientCall.Listener<ByteArray> {
        return clientListener
    }

    fun serverListener(): ServerCall.Listener<ByteArray> {
        return serverListener
    }
}
