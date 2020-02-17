package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single

internal interface ChunkDescriptor {

    object ChunkHasAlreadyBeenReadException : Exception()

    /**
     * Lazy representation of actual data chunk stored in-memory or on disk.
     *
     * Implementation must signal onError with [ChunkHasAlreadyBeenReadException] if client tries to read the same chunk twice.
     * Implementation must call [delete] once the chunk has been emitted to the client.
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
