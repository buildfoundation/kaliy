package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single

internal interface Storage {
    fun hold(chunk: ByteArray): Single<ChunkDescriptor>
}
