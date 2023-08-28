package io.kanro.mediator.internal

import com.bybutter.sisyphus.string.randomString
import com.bybutter.sisyphus.string.toPascalCase
import com.bybutter.sisyphus.string.toTitleCase
import io.grpc.Metadata
import io.grpc.Status
import io.kanro.mediator.desktop.model.CallTimeline
import io.kanro.mediator.desktop.model.MethodType
import kotlinx.coroutines.delay
import kotlin.random.Random

private val words =
    Thread.currentThread().contextClassLoader.getResourceAsStream("word.txt")?.reader()?.readLines() ?: listOf()

private val randomWord get() = words.random()

private val randomHost get() = "$randomWord.$randomWord.${domain.random()}"

private val methods = listOf(
    "Get", "List", "Update", "Delete"
)

private val randomMethod: String
    get() {
        val resource = randomWord.toPascalCase()
        val version = "v${Random.nextInt(10)}"

        return buildString {
            repeat(Random.nextInt(1, 4)) {
                append("$randomWord.")
            }
            append(version)
            append(".${resource}Api/")
            append(methods.random())
            append(resource)
        }
    }

private val domain = listOf(
    "com", "cn", "net", "org", "xyz", "me", "io", "dev"
)

fun randomKey(): Metadata.Key<String> {
    return when (Random.nextInt(3)) {
        0 -> Metadata.Key.of("x-$randomWord-$randomWord", Metadata.ASCII_STRING_MARSHALLER)
        1 -> Metadata.Key.of("x-$randomWord", Metadata.ASCII_STRING_MARSHALLER)
        else -> Metadata.Key.of("grpc-$randomWord", Metadata.ASCII_STRING_MARSHALLER)
    }
}

fun randomMetadata(): Metadata {
    return Metadata().apply {
        repeat(Random.nextInt(10)) {
            when (Random.nextInt(3)) {
                0 -> put(randomKey(), Random.nextInt(100).toString())
                1 -> put(randomKey(), randomWord)
                2 -> put(randomKey(), randomString(Random.nextInt(100)))
            }
        }
    }
}

fun randomDescriptor(): String {
    val words = mutableListOf<String>()
    repeat(Random.nextInt(3, 10)) {
        words += randomWord
    }
    return words.joinToString(" ").toTitleCase()
}

suspend fun randomCall(): CallTimeline {
    val method = randomMethod
    val host = "$randomHost:${Random.nextInt(1000, 9999)}"

    return CallTimeline().apply {
        this.start(
            Random.nextBoolean(), host, host, randomMethod, MethodType.UNKNOWN, randomMetadata(),
            "L:debug = R:debug",
            "L:debug = R:debug",
        )
    }
}

suspend fun emitCall(timeline: CallTimeline) {
    delay(Random.nextLong(1, 30))
    timeline.input(byteArrayOf())
    delay(Random.nextLong(1, 30))
    timeline.accept(randomMetadata())
    delay(Random.nextLong(1, 30))
    timeline.output(byteArrayOf())
    delay(Random.nextLong(1, 30))

    repeat(Random.nextInt(0, 3)) {
        timeline.input(byteArrayOf())
        delay(Random.nextLong(1, 30))
        timeline.output(byteArrayOf())
        delay(Random.nextLong(1, 30))
    }

    timeline.close(
        if (Random.nextInt(3) >= 2) {
            Status.fromCodeValue(Random.nextInt(17)).withDescription(randomDescriptor())
        } else {
            Status.OK.withDescription(randomDescriptor())
        },
        randomMetadata()
    )
}
