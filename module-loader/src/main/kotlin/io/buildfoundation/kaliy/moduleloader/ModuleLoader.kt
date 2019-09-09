package io.buildfoundation.kaliy.moduleloader

import io.buildfoundation.kaliy.cachelayer.CacheLayer
import io.buildfoundation.kaliy.config.Config
import io.buildfoundation.kaliy.http.api.HttpHandler

sealed class ModuleLoadResult<T> {
    data class Ok<T>(val instance: T) : ModuleLoadResult<T>()
    data class Error(val cause: Throwable) : ModuleLoadResult<Nothing>()
}

fun loadCacheLayer(config: Config.Layer): ModuleLoadResult<out CacheLayer> = loadClass(config.`class`, config)

fun loadHttpHandler(config: Config.Http.Handler): ModuleLoadResult<out HttpHandler> = loadClass(config.`class`, config)

private inline fun <reified ConfigType, reified ReturnType> loadClass(className: String, config: ConfigType): ModuleLoadResult<out ReturnType> = try {
    Class
            .forName(className)
            .constructors
            .firstOrNull { it.parameters.size == 1 && it.parameterTypes[0].isAssignableFrom(ConfigType::class.java) }
            .let {
                it
                        ?: throw IllegalStateException("Class $className does not have a constructor that takes ${ConfigType::class.java.canonicalName}")
            }
            .let { it.newInstance(config) as ReturnType }
            .let { ModuleLoadResult.Ok(it) }
} catch (t: Throwable) {
    ModuleLoadResult.Error(t)
}



