package io.buildfoundation.kaliy.swap.impl

import io.buildfoundation.kaliy.swap.Swap
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleEmitter
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class SwapImpl(private val configuration: Configuration) : Swap {

    data class Configuration(
            val memoryCapacityBytes: Long,
            val diskDirectory: Path,
            val diskCapacityBytes: Long,
            val diskIoBufferBytes: Int = 4 * 1024,
            val ioScheduler: Scheduler
    )

    internal val inMemoryStorage = InMemoryStorage(configuration.memoryCapacityBytes)

    /**
     * [SwapImpl] requires exclusive access to [Configuration.diskDirectory] so that we can create unique file names
     * faster than with [java.io.File.createTempFile] (notoriously slow due to use of [java.security.SecureRandom]).
     */
    private val filenameGenerator = AtomicInteger()

    override fun hold(dataChunks: Flowable<ByteArray>): Single<Swap.DataDescriptor> = Single
            .create { emitter ->
                val terminatedEagerly = AtomicBoolean()

                val bytesTotal = AtomicLong()
                val chunksTotal = AtomicInteger()

                val chunkDescriptors = ConcurrentHashMap<Int, ChunkDescriptor?>()

                val disposable = CompositeDisposable().also { emitter.setDisposable(it) }

                disposable += dataChunks
                        .subscribeOn(configuration.ioScheduler)
                        .subscribe(
                                { chunk ->
                                    bytesTotal.addAndGet(chunk.size.toLong())
                                    val chunkIndex = chunksTotal.getAndIncrement()

                                    disposable += holdChunk(chunk)
                                            .subscribeOn(configuration.ioScheduler)
                                            .subscribe(
                                                    { chunkDescriptor ->
                                                        if (terminatedEagerly.get()) {
                                                            chunkDescriptor.delete()
                                                        } else {
                                                            chunkDescriptors[chunkIndex] = chunkDescriptor
                                                            emitDataDescriptorIfAllChunksReady(chunkDescriptors, bytesTotal, chunksTotal, emitter)
                                                        }
                                                    },
                                                    { error ->
                                                        terminatedEagerly.set(true)

                                                        deleteChunkDescriptors(chunkDescriptors)
                                                        emitter.tryOnError(error)
                                                    }
                                            )
                                },
                                { error ->
                                    terminatedEagerly.set(true)
                                    deleteChunkDescriptors(chunkDescriptors)

                                    emitter.tryOnError(error)
                                },
                                { // onComplete
                                    emitDataDescriptorIfAllChunksReady(chunkDescriptors, bytesTotal, chunksTotal, emitter)
                                }
                        )

            }

    private fun deleteChunkDescriptors(chunkDescriptors: ConcurrentHashMap<Int, ChunkDescriptor?>) {
        chunkDescriptors.values.forEach { chunkDescriptor ->
            chunkDescriptor?.delete()
        }
    }

    private fun emitDataDescriptorIfAllChunksReady(chunkDescriptors: ConcurrentHashMap<Int, ChunkDescriptor?>, bytesTotal: AtomicLong, chunksTotal: AtomicInteger, emitter: SingleEmitter<Swap.DataDescriptor>) {
        if (chunkDescriptors.keys.count() == chunksTotal.get()) {
            emitter.onSuccess(object : Swap.DataDescriptor {
                override val bytesTotal: Long = bytesTotal.get()

                override val chunks: Flowable<ByteArray>
                    get() {
                        val chunks = if (chunksTotal.get() > 0) Array<Flowable<ByteArray>>(chunksTotal.get()) { index ->
                            chunkDescriptors[index]!!.chunk.toFlowable().subscribeOn(configuration.ioScheduler)
                        } else Array<Flowable<ByteArray>>(1) { Flowable.just(ByteArray(0)) }

                        return Flowable
                                .concatArrayEager(
                                        /* maxConcurrency */ 2,
                                        /* prefetch */ 2,
                                        *chunks
                                )
                                .doOnError { deleteChunkDescriptors(chunkDescriptors) }
                    }
            })
        }
    }

    private fun holdChunk(chunk: ByteArray): Single<ChunkDescriptor> {
        return inMemoryStorage.hold(chunk)
    }
}
