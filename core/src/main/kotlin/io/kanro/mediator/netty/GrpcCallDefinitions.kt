package io.kanro.mediator.netty

object GrpcCallDefinitions {
    const val SCHEMA_HEADER = ":schema"
    const val METHOD_HEADER = ":method"
    const val AUTHORITY_HEADER = ":authority"
    const val PATH_HEADER = ":path"
    const val TE_HEADER = "te"
    const val TE_HEADER_VALUE = "trailers"
    const val CONTENT_TYPE_HEADER = "content-type"
    const val CONTENT_TYPE_VALUE = "application/grpc"
    const val GRPC_CODING_HEADER = "grpc-encoding"
    const val GRPC_ACCEPT_CODING_HEADER = "grpc-accept-encoding"
    const val ACCEPT_CODING_HEADER = "accept-encoding"
    const val GRPC_MESSAGE_TYPE_HEADER = "grpc-message-type"
    const val GRPC_TIMEOUT_HEADER = "grpc-timeout"
    const val USER_AGENT_HEADER = "user-agent"

    val grpcHeaders = setOf(
        SCHEMA_HEADER,
        METHOD_HEADER,
        AUTHORITY_HEADER,
        PATH_HEADER,
        TE_HEADER,
        CONTENT_TYPE_HEADER,
        GRPC_CODING_HEADER,
        GRPC_ACCEPT_CODING_HEADER,
        ACCEPT_CODING_HEADER,
        GRPC_MESSAGE_TYPE_HEADER,
        GRPC_TIMEOUT_HEADER,
        USER_AGENT_HEADER
    )
}
