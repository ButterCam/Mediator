package io.kanro.mediator.internal

import com.bybutter.sisyphus.io.toUnixPath
import com.bybutter.sisyphus.protobuf.LocalProtoReflection
import com.bybutter.sisyphus.protobuf.ProtoSupport
import com.bybutter.sisyphus.protobuf.dynamic.DynamicFileSupport
import com.bybutter.sisyphus.protobuf.primitives.FileDescriptorProto
import com.bybutter.sisyphus.protobuf.primitives.FileDescriptorSet
import com.bybutter.sisyphus.protoc.Protoc
import io.grpc.reflection.v1alpha.ServerReflection
import io.grpc.reflection.v1alpha.ServerReflectionRequest
import io.grpc.reflection.v1alpha.ServerReflectionResponse
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension

sealed class MediatorProtoReflection : LocalProtoReflection() {
    override fun findSupport(name: String): ProtoSupport<*>? {
        super.findSupport(name)?.let { return it }

        synchronized(this) {
            runBlocking {
                doResolve(name.trim('.'))
            }
        }

        return super.findSupport(name)
    }

    protected abstract suspend fun doResolve(name: String)
}

class ProtoRootReflection(private val roots: List<String>) : MediatorProtoReflection() {
    private fun flattenProtos(protoPath: List<Path>): List<String> {
        val result = mutableListOf<String>()
        protoPath.forEach { root ->
            Files.newDirectoryStream(root).forEach {
                flattenProtos(root, it, result)
            }
        }
        return result
    }

    private fun flattenProtos(root: Path, file: Path, result: MutableList<String>) {
        if (Files.isDirectory(file)) {
            Files.newDirectoryStream(file).forEach {
                flattenProtos(root, it, result)
            }
        } else if (file.extension == "proto") {
            result.add(root.relativize(file).toString().toUnixPath())
        }
    }

    private fun generate(protoPath: List<Path>): FileDescriptorSet {
        if (protoPath.isEmpty()) {
            return FileDescriptorSet()
        }

        val protos = flattenProtos(protoPath)
        val outputFile = Files.createTempFile("out", ".pb")
        val arguments = buildList {
            add("-o$outputFile")
            protoPath.forEach {
                add("-I${it.toAbsolutePath()}")
            }
            add("--include_imports")
            add("--include_source_info")
            addAll(protos.toSet())
        }.toTypedArray()
        Protoc.runProtoc(arguments)

        val bytes = outputFile.toFile().readBytes()
        return FileDescriptorSet.parse(bytes)
    }

    override suspend fun doResolve(name: String) {
        generate(roots.map { Path.of(it) }).file.forEach {
            register(DynamicFileSupport(it))
        }
    }
}

class FileDescriptorSetReflection(private val files: List<String>) : MediatorProtoReflection() {
    override suspend fun doResolve(name: String) {
        files.forEach {
            val set = FileDescriptorSet.parse(Files.readAllBytes(Path.of(it)))
            set.file.forEach {
                register(DynamicFileSupport(it))
            }
        }
    }
}

class ServerProtoReflection(channel: io.grpc.Channel) : MediatorProtoReflection() {

    private val client = ServerReflection.Client(channel)

    private val collectedFile = mutableSetOf<String>()

    override suspend fun doResolve(name: String) {
        reflect(
            ServerReflectionRequest {
                this.fileContainingSymbol = name
            }
        )
    }

    private fun containsFile(fileName: String): Boolean {
        return collectedFile.contains(fileName)
    }

    private suspend fun reflect(initRequest: ServerReflectionRequest) {
        val input = Channel<ServerReflectionRequest>(1) {
            throw IllegalStateException()
        }
        val output = client.serverReflectionInfo(input.consumeAsFlow())
        input.send(initRequest)
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
}
