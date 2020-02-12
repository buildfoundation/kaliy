package io.buildfoundation.kaliy.swap

import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Abstract Swap storage.
 *
 * Swap is a concept similar to OS swap subsystem where data is stored in-memory
 * if possible and offloaded to disk in case of memory pressure transparently for the clients.
 *
 * Swap acts as a temporary holder of the data, once data is read it can no longer be accessed.
 * Swap is not a cache but is like a really fast, non-blocking /tmp directory.
 *
 * Implementation is allowed to hold each chunk of a given DataDescriptor in different storage systems as it pleases,
 * ie effectively allowing one logical Data item be partially stored in-memory and partially on the disk.
 *
 * Requirements for an implementation:
 * - Implementation must be non-blocking
 * - Implementation must be thread-safe
 * - Implementation must be lazy (no IO or space allocations unless Reactive operation is actually subscribed to)
 * - Implementation must not enforce its own timeouts (controlled by Kaliy itself)
 * - Implementation must properly implement in-flight operation cancellation
 * - Implementation must not violate Reactive Streams contracts
 * - Implementation must use Reactive Streams error mechanics
 */
interface Swap {

    interface DataDescriptor {

        /**
         * Total number of bytes represented by this descriptor (sum of size of each chunk).
         */
        val bytesTotal: Long

        /**
         * Lazy ordered stream of data chunks.
         * Subscriber must expect rx errors.
         *
         * Subscribing second time will result in onError signal.
         */
        val chunks: Flowable<ByteArray>
    }

    /**
     * Lazily holds passed dataChunks in this swap (once client subscribes to the returned [Single]).
     *
     * @param dataChunks stream of data chunks that will be held in this swap.
     * @return a [DataDescriptor] that allows data to be read in chunks when needed.
     */
    fun hold(dataChunks: Flowable<ByteArray>): Single<DataDescriptor>
}
