package io.buildfoundation.kaliy.swap

import io.reactivex.Flowable
import io.reactivex.Single
import java.nio.ByteBuffer

interface SwapStorage {
    sealed class Data {
        sealed class Chunk {
            data class Ok(val byteBuffer: ByteBuffer): Chunk()
            class Er(val cause: Throwable): Chunk()
        }

        class Ok(val stream: Flowable<Chunk>) : Data()
        class Er(val cause: Throwable) : Data()
    }

    fun put(stream: Flowable<ByteBuffer>, bytesCount: Long): Single<Data>
}
