package io.kanro.mediator.proxy.backend

import io.netty.bootstrap.Bootstrap

interface BootstrapHandler {
    fun handle(bootstrap: Bootstrap): Bootstrap
}
