package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single

internal interface Storage {

    abstract class NotEnoughStorageException: Exception()

    /**
     * Implementation must signal onError with [NotEnoughStorageException] if it runs out of desired capacity.
     */
    fun hold(chunk: ByteArray): Single<ChunkDescriptor>
}
