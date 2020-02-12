package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

internal class InMemoryStorage(private val capacityBytes: Long) : Storage {

    // Reuse same exception to avoid allocation and stacktrace re-capturing.
    private object NotEnoughMemoryException : Exception()

    private val utilizedBytes = AtomicLong()

    override fun hold(chunk: ByteArray): Single<ChunkDescriptor> = Single
            .create { emitter ->
                var ub: Long
                var newUb: Long

                do {
                    ub = utilizedBytes.get()
                    newUb = ub + chunk.size

                    if (newUb > capacityBytes) {
                        emitter.tryOnError(NotEnoughMemoryException)
                        return@create
                    }
                } while (!utilizedBytes.compareAndSet(ub, newUb))

                emitter.onSuccess(chunkDescriptor(chunk))
            }

    private fun chunkDescriptor(chunk: ByteArray): ChunkDescriptor {
        return object : ChunkDescriptor {
            val deleted = AtomicBoolean()

            override val chunk: Single<ByteArray> = Single
                    .create { emitter ->
                        delete()
                        emitter.onSuccess(chunk)
                    }

            override fun delete() {
                if (deleted.compareAndSet(false, true)) {
                    var cub = utilizedBytes.get()

                    while (!utilizedBytes.compareAndSet(cub, cub - chunk.size)) {
                        cub = utilizedBytes.get()
                    }
                }
            }
        }
    }
}
