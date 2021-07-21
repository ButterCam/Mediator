package io.kanro.mediator.internal

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.kanro.mediator.desktop.model.CallEvent
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.MethodType
import io.kanro.mediator.desktop.viewmodel.MainViewModel

class GrpcCallLogger(private val vm: MainViewModel) : ServerInterceptor {
    override fun <ReqT : Any, RespT : Any> interceptCall(
        call: ServerCall<ReqT, RespT>,
        headers: Metadata,
        next: ServerCallHandler<ReqT, RespT>
    ): ServerCall.Listener<ReqT> {
        val timeline = CallTimeline()
        timeline.emit(
            CallEvent.Start(
                call.authority ?: "Unknown",
                call.methodDescriptor.fullMethodName,
                MethodType(call.methodDescriptor.type),
                headers
            )
        )
        if (vm.recoding.value) {
            vm.calls += timeline
            val filter = vm.filter.value
            if (filter.isEmpty() || timeline.start().authority.contains(filter) || timeline.start().method.contains(
                    filter
                )
            ) {
                vm.shownCalls += timeline
            }
        }
        return Contexts.interceptCall(
            Context.current().withValue(timelineKey, timeline),
            GrpcLoggingCall(call),
            headers
        ) { call, headers ->
            GrpcLoggingCallListener(next.startCall(call, headers), call)
        }
    }

    companion object {
        val timelineKey = Context.key<CallTimeline>(CallTimeline::class.java.canonicalName)
    }
}
