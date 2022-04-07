package io.kanro.mediator.utils

import com.bybutter.sisyphus.security.base64
import com.bybutter.sisyphus.security.base64Decode
import io.grpc.Metadata

fun Metadata.toMap(): Map<String, String?> {
    return keys().associate {
        if (it.endsWith("-bin")) {
            it to this[Metadata.Key.of(it, Metadata.BINARY_BYTE_MARSHALLER)]?.base64()
        } else {
            it to this[Metadata.Key.of(it, Metadata.ASCII_STRING_MARSHALLER)]
        }
    }
}

fun Metadata.applyMap(data: Map<String, String?>): Metadata {
    data.forEach { (k, v) ->
        if (v == null) return@forEach
        if (k.endsWith("-bin")) {
            this.put(Metadata.Key.of(k, Metadata.BINARY_BYTE_MARSHALLER), v.base64Decode())
        } else {
            this.put(Metadata.Key.of(k, Metadata.ASCII_STRING_MARSHALLER), v)
        }
    }
    return this
}
