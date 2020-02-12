package io.buildfoundation.kaliy.cachelayers.inmemory

import io.buildfoundation.kaliy.cachelayer.CacheLayer
import io.buildfoundation.kaliy.config.Config
import io.buildfoundation.kaliy.swap.Swap
import io.reactivex.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory cache layer.
 */
class InMemoryCacheLayer(config: Config.Layer) : CacheLayer(config) {

    private val cache = ConcurrentHashMap<String, ByteArray>(64)
    private val sizeBytes = AtomicLong()

    override fun get(key: String): Single<GetResult> = Single.fromCallable {
        val value = cache[key]

        if (value == null) {
            GetResult.Miss(key)
        } else {
            GetResult.Hit(key, Swap.Data.Ok())
        }
    }

    override fun put(key: String, value: Swap.Data): Single<PutResult> {
        TODO("Not yet implemented")
    }
}
