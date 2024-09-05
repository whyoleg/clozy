/*
 * Copyright (c) 2024 Oleg Yukhnevich. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.whyoleg.clozy

import java.lang.ref.*
import kotlin.concurrent.*
import java.lang.ref.Cleaner as JdkCleaner

public actual open class SafeCloseable actual constructor(closeAction: SafeCloseAction) : AutoCloseable {
    private val handler = JvmCloseHandler(closeAction)

    @Suppress("LeakingThis")
    private val cleanable = cleaner.register(this, handler)

    public actual final override fun close() {
        handler.setDirectClose()
        cleaner.clean(cleanable)
    }
}

private class JvmCloseHandler(closeAction: SafeCloseAction) : CloseHandler(closeAction), Runnable {
    override fun run(): Unit = callClose()
}

private val cleaner: Cleaner = runCatching { Class.forName("java.lang.ref.Cleaner") }.fold(
    onSuccess = { JdkBasedCleaner },
    onFailure = { PhantomCleaner }
)

private abstract class Cleaner {
    abstract fun register(obj: Any, cleanup: Runnable): Any
    abstract fun clean(cleanable: Any)
}

@Suppress("Since15")
private object JdkBasedCleaner : Cleaner() {
    private val cleaner = JdkCleaner.create()
    override fun register(obj: Any, cleanup: Runnable): Any = cleaner.register(obj, cleanup)
    override fun clean(cleanable: Any): Unit = (cleanable as JdkCleaner.Cleanable).clean()
}

// TODO decide on reachabilityFence and overall regarding this implementation
//based on JDK and skiko impl
private object PhantomCleaner : Cleaner() {
    private val queue = ReferenceQueue<Any>()
    private val root = Cleanable()

    init {
        Cleanable(root, this, queue) { println("Cleaner Gone") }
        thread(start = true, isDaemon = true, name = "Reference Cleaner") {
            while (!root.isEmpty()) try {
                (queue.remove(60 * 1000L) as Cleanable?)?.clean()
            } catch (_: Throwable) {
            }
        }
    }

    override fun register(obj: Any, cleanup: Runnable): Any = Cleanable(root, obj, queue, cleanup)
    override fun clean(cleanable: Any): Unit = (cleanable as Cleanable).clean()

    private class Cleanable : PhantomReference<Any> {
        private val list: Cleanable
        private val action: Runnable?

        private var prev: Cleanable = this
        private var next: Cleanable = this

        //root
        constructor() : super(null, null) {
            this.list = this
            this.action = null
        }

        //node
        constructor(
            root: Cleanable,
            obj: Any,
            queue: ReferenceQueue<Any>,
            action: Runnable?,
        ) : super(obj, queue) {
            this.list = root
            this.action = action
            insert()
//            reachabilityFence(obj)
//            reachabilityFence(cleaner)
        }

        fun clean() {
            if (remove()) {
                super.clear()
                action?.run()
            }
        }

        override fun clear() {
            throw UnsupportedOperationException("clear() unsupported")
        }

        fun isEmpty(): Boolean = synchronized(list) { list === list.next }

        private fun insert(): Unit = synchronized(list) {
            prev = list
            next = list.next
            next.prev = this
            list.next = this
        }

        private fun remove(): Boolean = synchronized(list) {
            if (next !== this) {
                next.prev = prev
                prev.next = next
                prev = this
                next = this
                true
            } else {
                false
            }
        }
    }
}
