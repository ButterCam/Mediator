package io.kanro.mediator.internal

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.kanro.mediator.desktop.model.ServerRule
import io.kanro.mediator.utils.applyMap

class ReflectionAuth(private val rule: ServerRule?) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: io.grpc.Channel
    ): ClientCall<ReqT, RespT> {
        if (method.fullMethodName == "grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo") {
            return AuthCall(next.newCall(method, callOptions), rule)
        } else {
            return next.newCall(method, callOptions)
        }
    }

    class AuthCall<ReqT, RespT>(delegate: ClientCall<ReqT, RespT>, private val rule: ServerRule?) :
        ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(delegate) {
        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            rule?.metadata?.let {
                headers.applyMap(it)
            }
            delegate().start(responseListener, headers)
        }
    }
}
