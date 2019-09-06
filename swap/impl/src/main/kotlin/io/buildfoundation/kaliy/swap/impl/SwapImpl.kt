package io.buildfoundation.kaliy.swap.impl

import io.buildfoundation.kaliy.swap.Swap
import io.reactivex.Flowable
import io.reactivex.Single
import java.io.File
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class SwapImpl(private val configuration: Configuration) : Swap {

    data class Configuration(
            val diskDirectory: Path,
            val diskDataAliveTimeoutMillis: Long,
            val diskBufferBytes: Int = 4 * 1024
    )

    /**
     * [SwapImpl] requires exclusive access to [Configuration.diskDirectory] so that we can create unique file names
     * cheaper than with [java.io.File.createTempFile].
     */
    private val filenameGenerator = AtomicInteger()

    override fun from(bytesCount: Long, stream: Flowable<ByteBuffer>): Single<Swap.Data> {
        if (bytesCount > Int.MAX_VALUE) {
            TODO()
        } else {
            return toMemory(bytesCount.toInt(), stream)
        }
    }

    private fun toMemory(bytesCount: Int, stream: Flowable<ByteBuffer>): Single<Swap.Data> {
        return stream
                .collect({ ByteBuffer.allocate(bytesCount) }, { data, buf -> data.put(buf) })
                .map<Swap.Data> { Swap.Data.Ok(Flowable.just(Swap.Data.Chunk.Ok(it))) }
                .onErrorReturn { Swap.Data.Er(it) }
    }

    private fun toDisk(bytesCount: Int, stream: Flowable<ByteBuffer>): Single<Swap.Data> {
        return Single
                .using(
                        {
                            val file = configuration.diskDirectory.resolveSibling("${filenameGenerator.incrementAndGet()}").toFile()
                            val outputStream = file.outputStream().buffered(configuration.diskBufferBytes)

                            outputStream to file
                        },
                        { (outputStream, file) ->
                            val tmpArray = ByteArray(configuration.diskBufferBytes)
                            stream
                                    .collect({ Unit }, { _, buf -> buf.writeTo(outputStream, tmpArray) })
                                    .map { Swap.Data.Ok(file.toChunks(configuration.diskBufferBytes)) }
                        },
                        { (os) -> os.close() }
                )
    }
}

private fun ByteBuffer.writeTo(out: OutputStream, tmpArray: ByteArray) {
    val buffer = this
    val remaining = buffer.remaining()

    if (buffer.hasArray()) {
        out.write(buffer.array(), buffer.arrayOffset() + buffer.position(), remaining)
        buffer.position(buffer.position() + remaining)
    } else {
        while (buffer.hasRemaining()) {
            val byteCountToWrite = Math.min(remaining, tmpArray.size)
            buffer.get(tmpArray, 0, byteCountToWrite)
            out.write(tmpArray, 0, byteCountToWrite)
        }
    }
}

private fun File.toChunks(diskBufferBytes: Int): Flowable<Swap.Data.Chunk> {
    return Flowable
            .using(
                    { RandomAccessFile(this, "r").channel },
                    {
                        Flowable.generate<Swap.Data.Chunk> { emitter ->
                            val buffer = ByteBuffer.allocate(diskBufferBytes)
                            it.read(buffer)

                            emitter.onNext(Swap.Data.Chunk.Ok(buffer))
                        }
                    },
                    { it.close() }
            )
}
