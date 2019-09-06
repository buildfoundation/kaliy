package io.buildfoundation.kaliy.swap.impl

import io.buildfoundation.kaliy.swap.SwapStorage
import io.reactivex.Flowable
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.util.*

class SwapStorageImplTest {

    @get:Rule
    val dir = TemporaryFolder()

    @Test
    fun `put empty`() {
        val swap = SwapStorageImpl(configuration = SwapStorageImpl.Configuration(
                diskDirectory = dir.root.toPath(),
                diskDataAliveTimeoutMillis = 0L,
                diskBufferBytes = 0
        ))
        val values = swap.put(Flowable.empty(), 0)
                .test()
                .values()

        val actual = values.first()
        assertThat(actual).isInstanceOf(SwapStorage.Data.Ok::class.java)

        val ok = actual as SwapStorage.Data.Ok
        ok.stream
                .test()
                .assertValue(SwapStorage.Data.Chunk.Ok(ByteBuffer.allocate(0)))
    }

    @Test
    fun put() {
        val swap = SwapStorageImpl(configuration = SwapStorageImpl.Configuration(
                diskDirectory = dir.root.toPath(),
                diskDataAliveTimeoutMillis = 0L,
                diskBufferBytes = 0
        ))

        val bytes = "Some text".toByteArray()
        val byteBuffer = ByteBuffer.allocate(bytes.size)
        byteBuffer.put(bytes)

        val values = swap.put(Flowable.just(byteBuffer), bytes.size.toLong())
                .test()
                .also { it.awaitTerminalEvent() }
                .assertValueCount(1)
                .values()

        val actualData = values.first()
        assertThat(actualData).isInstanceOf(SwapStorage.Data.Ok::class.java)

        val ok = actualData as SwapStorage.Data.Ok
        val test = ok.stream.test()
        val actualChunk = test.values().first()

        assertThat(actualChunk).isInstanceOf(SwapStorage.Data.Chunk.Ok::class.java)

        assertThat(actualChunk)
                .matches( { chunk ->
                    Arrays.equals(
                            (chunk as SwapStorage.Data.Chunk.Ok)
                                    .byteBuffer
                                    .array(),
                            byteBuffer.array()
                    )
                }, "ByteBuffer content")
    }
}
