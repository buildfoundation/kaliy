package io.buildfoundation.kaliy.cachelayer

import io.buildfoundation.kaliy.config.Config
import io.buildfoundation.kaliy.swap.Swap
import io.reactivex.Single

/**
 * Abstract cache layer.
 *
 * Requirements for an implementation:
 * - Implementation must be non-blocking
 * - Implementation must be thread-safe
 * - Implementation must be lazy (no data is read or storage is allocated unless Reactive operation is actually subscribed to)
 * - Implementation must not enforce its own timeouts (controlled by Kaliy itself)
 * - Implementation must properly implement in-flight operation cancellation
 * - Implementation must not violate Reactive Streams contracts
 */
abstract class CacheLayer(protected val config: Config.Layer) {

    sealed class GetResult(open val key: String) {
        class Hit(override val key: String, value: Swap.Data) : GetResult(key)
        data class Miss(override val key: String) : GetResult(key)
        class Error(override val key: String, val cause: Throwable) : GetResult(key)
    }

    sealed class PutResult(open val key: String) {
        data class Ok(override val key: String) : PutResult(key)
        class Error(override val key: String, val cause: Throwable) : PutResult(key)
    }

    /**
     * Asynchronously gets result from this cache layer.
     */
    abstract fun get(key: String): Single<GetResult>

    /**
     * Asynchronously puts key/value pair to this cache layer.
     */
    abstract fun put(key: String, value: Swap.Data): Single<PutResult>
}
