package io.kanro.mediator.proxy.frontend

import io.netty.bootstrap.ServerBootstrap

interface BootstrapHandler {
    fun handle(bootstrap: ServerBootstrap): ServerBootstrap
}
