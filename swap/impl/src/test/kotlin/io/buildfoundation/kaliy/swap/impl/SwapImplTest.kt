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
                    assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(dir.root.listFiles()).isEmpty()

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

        assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
        assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
    }

    @Test
    fun holdOneChunkInMemory() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 10 * 1024 * 1024,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        val chunk = "Some text".toByteArray()
        val originalSizeBytes = chunk.size.toLong()

        swap
                .hold(Flowable.just(chunk))
                .flatMapPublisher {
                    assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(originalSizeBytes)
                    assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(dir.root.listFiles()).isEmpty()

                    assertThat(it.bytesTotal).isEqualTo(originalSizeBytes)
                    it.chunks
                }
                .test()
                .also { it.awaitTerminalEvent(5, SECONDS) }
                .assertNoErrors()
                .assertValueCount(1)
                .assertComplete()
                .values()
                .single()
                .also { actual ->
                    assertThat(chunk)
                            .describedAs("Chunk must be equal to original: expected = '${String(chunk)}', actual = '${String(actual)}'")
                            .isEqualTo(actual)
                }

        assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
        assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
    }

    @Test
    fun holdOneChunkOnDisk() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 0,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        val chunk = "Some text".toByteArray()
        val originalSizeBytes = chunk.size.toLong()

        swap
                .hold(Flowable.just(chunk))
                .flatMapPublisher {
                    assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(originalSizeBytes)

                    assertThat(it.bytesTotal).isEqualTo(originalSizeBytes)
                    it.chunks
                }
                .test()
                .also { it.awaitTerminalEvent(5, SECONDS) }
                .assertNoErrors()
                .assertValueCount(1)
                .assertComplete()
                .values()
                .single()
                .also { actual ->
                    assertThat(chunk)
                            .describedAs("Chunk must be equal to original: expected = '${String(chunk)}', actual = '${String(actual)}'")
                            .isEqualTo(actual)
                }

        assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
        assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
    }

    @Test
    fun holdMultipleChunksInMemory() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 10 * 1024 * 1024,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        val originalChunks = listOf("Some", "test", "content").map { it.toByteArray() }
        val originalSizeBytes = originalChunks.sumBy { it.size }.toLong()

        val chunksFromSwap = swap
                .hold(Flowable.fromIterable(originalChunks))
                .flatMapPublisher { dataDescriptor ->
                    assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(originalSizeBytes)
                    assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(dir.root.listFiles()).isEmpty()

                    assertThat(dataDescriptor.bytesTotal).isEqualTo(originalSizeBytes)
                    dataDescriptor.chunks
                }
                .test()
                .also { it.awaitTerminalEvent(5, SECONDS) }
                .assertNoErrors()
                .assertValueCount(originalChunks.size)
                .assertComplete()
                .values()

        chunksFromSwap.forEachIndexed { index, actual ->
            val expected = originalChunks[index]

            assertThat(expected)
                    .describedAs("Chunk at index $index must be equal to original: expected = '${String(expected)}', actual = '${String(actual)}'")
                    .isEqualTo(actual)
        }

        assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
        assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
    }

    @Test
    fun holdMultipleChunksOnDisk() {
        val swap = SwapImpl(configuration = SwapImpl.Configuration(
                memoryCapacityBytes = 0,
                diskDirectory = dir.root.toPath(),
                diskCapacityBytes = 20 * 1024 * 1024,
                ioScheduler = Schedulers.io()
        ))

        val originalChunks = listOf("Some", "test", "content").map { it.toByteArray() }
        val originalSizeBytes = originalChunks.sumBy { it.size }.toLong()

        val chunksFromSwap = swap
                .hold(Flowable.fromIterable(originalChunks))
                .flatMapPublisher { dataDescriptor ->
                    assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
                    assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(originalSizeBytes)

                    assertThat(dataDescriptor.bytesTotal).isEqualTo(originalSizeBytes)
                    dataDescriptor.chunks
                }
                .test()
                .also { it.awaitTerminalEvent(5, SECONDS) }
                .assertNoErrors()
                .assertValueCount(originalChunks.size)
                .assertComplete()
                .values()

        chunksFromSwap.forEachIndexed { index, actual ->
            val expected = originalChunks[index]

            assertThat(expected)
                    .describedAs("Chunk at index $index must be equal to original: expected = '${String(expected)}', actual = '${String(actual)}'")
                    .isEqualTo(actual)
        }

        assertThat(swap.inMemoryStorage.utilizedBytes.get()).isEqualTo(0L)
        assertThat(swap.diskStorage.utilizedBytes.get()).isEqualTo(0L)
    }


}
