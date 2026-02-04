package com.vwatek.apply.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Wrapper to observe StateFlow from iOS/Swift
 * Provides a simple callback-based API that works well with Swift
 */
class Closeable(
    private val job: Job
) {
    fun close() {
        job.cancel()
    }
}

/**
 * Extension function to watch StateFlow from Swift
 * Returns a Closeable that can be used to stop watching
 */
fun <T> StateFlow<T>.watch(block: (T) -> Unit): Closeable {
    val job = this.onEach(block)
        .launchIn(CoroutineScope(Dispatchers.Main))
    return Closeable(job)
}
