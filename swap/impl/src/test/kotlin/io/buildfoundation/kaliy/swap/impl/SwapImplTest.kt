package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.TimeUnit.SECONDS

class SwapImplTest {

    @get:Rule
    val dir = TemporaryFolder()

    @Test
    fun hold0Chunks() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 10 * 1024 * 1024,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        swap
                .hold(Flowable.empty())
                .flatMapPublisher {
                    assertThat(it.bytesTotal).isEqualTo(0)
                    it.chunks
                }
                .test()
                .also {
                    it.awaitTerminalEvent(5, SECONDS)
                }
                .assertNoErrors()
                .assertValueCount(1)
                .assertComplete()
                .assertValue { chunk -> chunk.isEmpty() }
    }

    @Test
    fun holdOneChunk() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 10 * 1024 * 1024,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        val bytes = "Some text".toByteArray()

        swap
                .hold(Flowable.just(bytes))
                .flatMapPublisher {
                    assertThat(it.bytesTotal).isEqualTo(bytes.size.toLong())
                    it.chunks
                }
                .test()
                .also { it.awaitTerminalEvent(5, SECONDS) }
                .assertNoErrors()
                .assertValueCount(1)
                .assertComplete()
                .assertValue { chunk -> bytes.contentEquals(chunk) }
    }
}
