/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

public actual open class SafeCloseable actual constructor(closeAction: SafeCloseAction) : AutoCloseable {
    private val handler = JsCloseHandler(closeAction)

    init {
        @Suppress("LeakingThis")
        finalizationRegistry.register(this, handler, this)
    }

    actual final override fun close() {
        finalizationRegistry.unregister(this)
        handler.setDirectClose()
        handler.close()
    }
}

private class JsCloseHandler(closeAction: SafeCloseAction) : CloseHandler(closeAction) {
    private var executed = false
    fun close() {
        if (executed) return
        executed = true
        callClose()
    }
}

private external class FinalizationRegistry(cleanup: (handler: JsCloseHandler) -> Unit) {
    fun register(obj: SafeCloseable, handler: JsCloseHandler, token: SafeCloseable)
    fun unregister(token: SafeCloseable)
}

private val finalizationRegistry = FinalizationRegistry(JsCloseHandler::close)
