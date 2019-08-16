package io.buildfoundation.kaliy.swap

import io.reactivex.Flowable
import io.reactivex.Single
import java.nio.ByteBuffer

interface Swap {
    sealed class Data {
        sealed class Chunk {
            class Ok(val byteBuffer: ByteBuffer): Chunk()
            class Er(val cause: Throwable): Chunk()
        }

        class Ok(val stream: Flowable<Chunk>) : Data()
        class Er(val cause: Throwable) : Data()
    }

    fun from(bytesCount: Long, stream: Flowable<ByteBuffer>): Single<Data>
}
