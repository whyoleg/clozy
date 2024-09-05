/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

// no support for auto clean
public actual open class SafeCloseable actual constructor(private val closeAction: SafeCloseAction) : AutoCloseable {
    private var executed = false
    actual final override fun close() {
        if (executed) return
        executed = true
        closeAction.onDirectClose()
    }
}
