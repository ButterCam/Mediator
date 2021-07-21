package io.kanro.mediator.internal

import com.bybutter.sisyphus.protobuf.LocalProtoReflection
import com.bybutter.sisyphus.protobuf.dynamic.DynamicFileSupport
import com.bybutter.sisyphus.protobuf.primitives.FileDescriptorProto
import io.grpc.reflection.v1alpha.ServerReflection
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking

class StatefulProtoReflection(channel: io.grpc.Channel) : LocalProtoReflection() {
    private var collected = false

    private val client = ServerReflection.Client(channel)

    private val collectedFile = mutableSetOf<String>()

    fun resolved(): Boolean {
        return collected
    }

    fun collect(): StatefulProtoReflection {
        if (collected) return this
        synchronized(this) {
            if (collected) return this
            try {
                runBlocking {
                    val input = Channel<ServerReflectionRequest>(1) {
                        throw IllegalStateException()
                    }
                    val output = client.serverReflectionInfo(input.consumeAsFlow())
                    input.send(
                        ServerReflectionRequest {
                            listServices = ""
                        }
                    )
                    var count = 1
                    output.collect {
                        count--
                        when (val response = it.messageResponse) {
                            is ServerReflectionResponse.MessageResponse.AllExtensionNumbersResponse -> TODO()
                            is ServerReflectionResponse.MessageResponse.ErrorResponse -> TODO()
                            is ServerReflectionResponse.MessageResponse.FileDescriptorResponse -> {
                                for (descriptor in response.value.fileDescriptorProto) {
                                    val file = FileDescriptorProto.parse(descriptor)
                                    collectedFile += file.name
                                    if (findSupport(file.name) == null) {
                                        register(DynamicFileSupport(file))
                                    }
                                    for (dependency in file.dependency) {
                                        if (!containsFile(dependency) && findSupport(dependency) == null) {
                                            collectedFile += dependency
                                            input.send(
                                                ServerReflectionRequest {
                                                    println("Send: $dependency")
                                                    this.fileByFilename = dependency
                                                }
                                            )
                                            count++
                                        }
                                    }
                                }
                            }
                            is ServerReflectionResponse.MessageResponse.ListServicesResponse -> {
                                for (service in response.value.service) {
                                    input.send(
                                        ServerReflectionRequest {
                                            this.fileContainingSymbol = service.name
                                        }
                                    )
                                    count++
                                }
                            }
                            null -> TODO()
                        }
                        if (count == 0) {
                            input.close()
                        }
                    }
                }
                collected = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return this
    }

    private fun containsFile(fileName: String): Boolean {
        return collectedFile.contains(fileName)
    }
}
