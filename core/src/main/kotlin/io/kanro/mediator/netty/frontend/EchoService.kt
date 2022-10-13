package io.kanro.mediator.netty.frontend

import io.kanro.mediator.netty.GrpcProxySupport
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.DefaultFullHttpResponse
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import java.io.StringWriter
import java.nio.charset.Charset

object EchoService {
    fun buildEchoResponse(context: ChannelHandlerContext, request: HttpRequest): HttpResponse {
        if (request.method() == HttpMethod.GET) {
            val support = context.channel().attr(GrpcProxySupport.KEY).get()
            val certificate = support.getCertificateAuthority()
            val path = request.uri().substringAfterLast('/')
            when (path) {
                "mediatorRoot.cer" -> {
                    return DefaultFullHttpResponse(
                        request.protocolVersion(),
                        HttpResponseStatus.OK,
                        context.alloc().buffer().writeBytes(certificate.encoded).retain()
                    )
                }

                "mediatorRoot.pem", "mediatorRoot.crt" -> {
                    val writer = StringWriter()
                    JcaPEMWriter(writer).apply {
                        writeObject(certificate)
                        flush()
                        close()
                    }
                    return DefaultFullHttpResponse(
                        request.protocolVersion(),
                        HttpResponseStatus.OK,
                        context.alloc().buffer().writeBytes(writer.toString().toByteArray(Charset.defaultCharset()))
                            .retain()
                    )
                }
            }
        }

        val body = html {
            head {
                title("Mediator Proxy")
            }
            body {
                h1("Mediator HTTP Echo Service")
                p("${request.method().name()} ${request.uri()}")
                request.headers().forEach { (k, v) ->
                    p("$k: $v")
                }
                hr()

                p {
                    text("Download the ")
                    a("./mediatorRoot.cer", "Mediator Root Certificate")
                }
            }
        }

        return DefaultFullHttpResponse(
            request.protocolVersion(),
            HttpResponseStatus.OK,
            context.alloc().buffer().writeBytes(body.toByteArray()).retain()
        )
    }
}

private class HtmlBuilder {
    private val builder = StringBuilder()

    private fun String.escapeHTML(): String? {
        return buildString {
            this@escapeHTML.forEach {
                when (it) {
                    '<' -> append("&lt;")
                    '>' -> append("&gt;")
                    '&' -> append("&amp;")
                    '"' -> append("&quot;")
                    '\'' -> append("&#x27;")
                    '/' -> append("&#x2F;")
                    else -> append(it)
                }
            }
        }
    }

    fun html(init: HtmlBuilder.() -> Unit): String {
        tag("html", init)
        return builder.toString()
    }

    fun head(init: HtmlBuilder.() -> Unit) {
        tag("head", init)
    }

    fun body(init: HtmlBuilder.() -> Unit) {
        tag("body", init)
    }

    fun title(content: String) {
        tag("title") {
            text(content)
        }
    }

    fun p(content: String) {
        tag("p") {
            text(content)
        }
    }

    fun p(init: HtmlBuilder.() -> Unit) {
        tag("p", init)
    }

    fun a(url: String, content: String) {
        tag("a", mapOf("href" to url)) {
            text(content)
        }
    }

    fun br() {
        builder.append("<br>")
    }

    fun hr() {
        builder.append("<hr>")
    }

    fun h1(content: String) {
        tag("h1") {
            text(content)
        }
    }

    fun text(text: String) {
        builder.append(text.escapeHTML())
    }

    operator fun String.invoke(init: HtmlBuilder.() -> Unit) {
        tag(this, init)
    }

    operator fun String.invoke(attributes: Map<String, String>, init: HtmlBuilder.() -> Unit) {
        tag(this, attributes, init)
    }

    fun tag(name: String, init: HtmlBuilder.() -> Unit) {
        tag(name, emptyMap(), init)
    }

    fun tag(name: String, attributes: Map<String, String>, init: HtmlBuilder.() -> Unit) {
        if (attributes.isEmpty()) {
            builder.append("<$name>")
        } else {
            val formattedAttributes = attributes.entries.joinToString(" ") { "${it.key}=\"${it.value.escapeHTML()}\"" }
            builder.append("<$name $formattedAttributes>")
        }
        init()
        builder.append("</$name>")
    }
}

private fun html(init: HtmlBuilder.() -> Unit): String {
    return HtmlBuilder().html(init)
}
