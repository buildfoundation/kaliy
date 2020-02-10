@file:JvmName("Main")

package io.buildfoundation.kaliy.main

import io.buildfoundation.kaliy.config.Config
import io.buildfoundation.kaliy.config.parseConfig
import io.buildfoundation.kaliy.handlers.gradle.GradleHttpHandler
import io.buildfoundation.kaliy.http.netty.httpServer
import io.buildfoundation.kaliy.moduleloader.loadCacheLayer
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlin.concurrent.thread

internal fun main(rawArgs: Array<String>) {
    thread(isDaemon = true) { Thread.sleep(Long.MAX_VALUE) /* Start daemon thread to prevent JVM from exiting */ }

    val defaultConfig = Config(
            http = Config.Http(
                    port = 8080,
                    handlers = listOf(
                            Config.Http.Handler(
                                    endpoint = "/gradle",
                                    `class` = GradleHttpHandler::class.java.canonicalName,
                                    comment = "",
                                    config = emptyMap()
                            )
                    )
            ),
            batching = Config.Batching(Config.Batching.Get(42L, 42), Config.Batching.Put(42L, 42)),
            layers = listOf()
    )

    Single
            .fromCallable { parseArgs(rawArgs) }
            .map { parseConfig(it.configFile) }
            .flatMap {
                Observable
                        .fromIterable(it.layers)
                        .flatMapSingle { layer ->
                            Single
                                    .fromCallable { loadCacheLayer(layer) }
                                    .subscribeOn(Schedulers.computation())
                        }
                        .toList()
            }
//            .subscribe()
//            .also { registerShutdownHook(it) }

    httpServer(defaultConfig.http).subscribe().also {
        registerShutdownHook(it)
    }
}

private fun registerShutdownHook(disposable: Disposable) {
    Runtime
            .getRuntime()
            .addShutdownHook(thread(start = false) {
                // TODO log
                disposable.dispose()
            })
}
