/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

import kotlin.concurrent.*
import kotlin.experimental.*
import kotlin.native.ref.*

public actual open class SafeCloseable actual constructor(closeAction: SafeCloseAction) : AutoCloseable {
    private val handler = NativeCloseHandler(closeAction)

    @OptIn(ExperimentalNativeApi::class)
    @Suppress("unused")
    private val cleaner = createCleaner(handler, NativeCloseHandler::close)

    actual final override fun close() {
        handler.setDirectClose()
        handler.close()
    }
}

private class NativeCloseHandler(closeAction: SafeCloseAction) : CloseHandler(closeAction) {
    private val executed = AtomicInt(0)
    fun close() {
        if (executed.compareAndSet(0, 1)) callClose()
    }
}
