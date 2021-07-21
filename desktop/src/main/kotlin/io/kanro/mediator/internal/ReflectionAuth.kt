package io.kanro.mediator.internal

import io.grpc.CallOptions
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.kanro.mediator.desktop.model.ServerRule
import io.kanro.mediator.utils.applyMap

class ReflectionAuth(private val rules: List<ServerRule>) : ClientInterceptor {
    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: io.grpc.Channel
    ): ClientCall<ReqT, RespT> {
        if (method.fullMethodName == "grpc.reflection.v1alpha.ServerReflection/ServerReflectionInfo") {
            return AuthCall(next.newCall(method, callOptions), rules)
        } else {
            return next.newCall(method, callOptions)
        }
    }

    class AuthCall<ReqT, RespT>(delegate: ClientCall<ReqT, RespT>, private val rules: List<ServerRule>) :
        ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(delegate) {
        override fun start(responseListener: Listener<RespT>, headers: Metadata) {
            rules.forEach {
                if (it.enabled) {
                    headers.applyMap(it.metadata)
                }
            }
            delegate().start(responseListener, headers)
        }
    }
}
