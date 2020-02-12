package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single

internal interface ChunkDescriptor {
    /**
     * Lazy representation of actual data chunk stored in-memory or on disk.
     *
     * Subscribing second time will result in onError signal.
     */
    val chunk: Single<ByteArray>

    /**
     * Deletes the data in the underlying storage held by this descriptor.
     * Safe to call multiple times or if the [chunk] has already been read.
     *
     * Subscribing to [chunk] stream after call to [delete] will result in onError signal.
     */
    fun delete()
}
