@file:JvmName("Main")

package io.buildfoundation.kaliy.main

import io.buildfoundation.kaliy.config.parseConfig
import io.buildfoundation.kaliy.http.netty.httpServer
import io.buildfoundation.kaliy.moduleloader.loadLayer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlin.concurrent.thread

internal fun main(rawArgs: Array<String>) {
    thread(isDaemon = true) { Thread.sleep(Long.MAX_VALUE) /* Start daemon thread to prevent JVM from exiting */ }

    Single
            .fromCallable { parseArgs(rawArgs) }
            .map { parseConfig(it.configFile) }
            .flatMap {
                Observable
                        .fromIterable(it.layers)
                        .flatMapSingle { layer ->
                            Single
                                    .fromCallable { loadLayer(layer) }
                                    .subscribeOn(Schedulers.computation())
                        }
                        .toList()
            }
//            .subscribe()
//            .also { registerShutdownHook(it) }

    registerShutdownHook(httpServer(8080).subscribe())
}

private fun registerShutdownHook(disposable: Disposable) {
    Runtime
            .getRuntime()
            .addShutdownHook(thread(start = false) {
                // TODO log
                disposable.dispose()
            })
}
