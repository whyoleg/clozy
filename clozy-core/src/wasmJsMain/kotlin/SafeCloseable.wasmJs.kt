/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

public actual open class SafeCloseable actual constructor(closeAction: SafeCloseAction) : AutoCloseable {
    private val handler = WasmJsCloseHandler(closeAction)
    private val jsReference = toJsReference()

    init {
        finalizationRegistry.register(jsReference, handler.toJsReference(), jsReference)
    }

    actual final override fun close() {
        finalizationRegistry.unregister(jsReference)
        handler.setDirectClose()
        handler.close()
    }
}

private class WasmJsCloseHandler(closeAction: SafeCloseAction) : CloseHandler(closeAction) {
    private var executed = false
    fun close() {
        if (executed) return
        executed = true
        callClose()
    }
}

private external class FinalizationRegistry(cleanup: (handler: JsReference<WasmJsCloseHandler>) -> Unit) {
    fun register(obj: JsReference<SafeCloseable>, handler: JsReference<WasmJsCloseHandler>, token: JsReference<SafeCloseable>)
    fun unregister(token: JsReference<SafeCloseable>)
}

private val finalizationRegistry = FinalizationRegistry { it.get().close() }
