package io.buildfoundation.kaliy.swap.impl

import io.reactivex.Single
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

internal class DiskStorage(private val directory: File, private val capacityBytes: Long) : Storage {

    // Reuse same exception to avoid allocation and stacktrace re-capturing.
    private object NotEnoughDiskException : Storage.NotEnoughStorageException()

    internal val utilizedBytes = AtomicLong()

    /**
     * [DiskStorage] requires exclusive access to [directory] so that we can create unique file names
     * faster than with [java.io.File.createTempFile] (notoriously slow due to use of [java.security.SecureRandom]).
     */
    private val filenameGenerator = AtomicInteger()

    init {
        directory.mkdirs()
        directory.deleteRecursively()
        directory.mkdirs()
    }

    override fun hold(chunk: ByteArray): Single<ChunkDescriptor> = Single
            .create { emitter ->
                var ub: Long
                var newUb: Long

                do {
                    ub = utilizedBytes.get()
                    newUb = ub + chunk.size

                    if (newUb > capacityBytes) {
                        emitter.tryOnError(NotEnoughDiskException)
                        return@create
                    }
                } while (!utilizedBytes.compareAndSet(ub, newUb))

                // TODO: write multiple chunks in same file(s)
                val file = File(directory, filenameGenerator.incrementAndGet().toString())

                // TODO: use io_uring on Linix
                file.writeBytes(chunk)

                emitter.onSuccess(DiskChunkDescriptor(file, chunk.size))
            }

    inner class DiskChunkDescriptor(private val file: File, private val sizeBytes: Int) : ChunkDescriptor {

        private val deleted = AtomicBoolean()

        override val chunk: Single<ByteArray> = Single
                .fromCallable {
                    // TODO use io_uring on Linux.

                    if (!file.exists()) {
                        throw ChunkDescriptor.ChunkHasAlreadyBeenReadException
                    }

                    try {
                        return@fromCallable file.readBytes()
                    } finally {
                        delete()
                    }
                }

        override fun delete() {
            if (deleted.compareAndSet(false, true)) {
                var cub = utilizedBytes.get()

                while (!utilizedBytes.compareAndSet(cub, cub - sizeBytes)) {
                    cub = utilizedBytes.get()
                }

                file.delete()
            }

        }
    }
}

