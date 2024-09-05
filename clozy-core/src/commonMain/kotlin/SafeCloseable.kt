/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

import kotlin.concurrent.*

public expect open class SafeCloseable(closeAction: SafeCloseAction) : AutoCloseable {
    public final override fun close()
}

public inline fun <T> SafeCloseable(resource: T, crossinline closeAction: (T) -> Unit): SafeCloseable =
    SafeCloseable(object : SafeCloseAction {
        override fun onClose(): Unit = closeAction(resource)
    })

// not a `fun` interface as better control of `captured` variables is required
// provides at-most-once guarantee
//  onAutoClose   will be called when collected by GC (if possible)
//  onDirectClose will be called when directly called `close`
public interface SafeCloseAction {
    public fun onClose()
    public fun onDirectClose(): Unit = onClose()
    public fun onAutoClose(): Unit = onClose()
}

internal abstract class CloseHandler(private val closeAction: SafeCloseAction) {
    @Volatile
    private var directClose = false

    fun setDirectClose() {
        directClose = true
    }

    fun callClose(): Unit = when {
        directClose -> closeAction.onDirectClose()
        else        -> closeAction.onAutoClose()
    }
}
