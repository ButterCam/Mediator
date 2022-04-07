package io.kanro.mediator.internal

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.MethodType
import io.kanro.mediator.desktop.viewmodel.MainViewModel

class GrpcCallLogger : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val timeline = CallTimeline()
        val rules = MainViewModel.configuration.requestRules.filter {
            it.enabled && it.method == call.methodDescriptor.fullMethodName
        }

        val start = timeline.start(
            call.authority ?: "Unknown",
            call.methodDescriptor.fullMethodName,
            MethodType(call.methodDescriptor.type),
            headers, rules
        )

        if (MainViewModel.recoding.value) {
            MainViewModel.calls += timeline
            val filter = MainViewModel.filter.value
            if (filter.isEmpty() || timeline.start().authority.contains(filter) || timeline.start().method.contains(
                    filter
                )
            ) {
                MainViewModel.shownCalls += timeline
            }
        }
        return Contexts.interceptCall(
            Context.current().withValue(timelineKey, timeline),
            GrpcLoggingCall(call),
            start.header
        ) { call, headers ->
            GrpcLoggingCallListener(next.startCall(call, headers), call)
        }
    }

    companion object {
        val timelineKey = Context.key<CallTimeline>(CallTimeline::class.java.canonicalName)
    }
}
