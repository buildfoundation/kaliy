package io.buildfoundation.kaliy.moduleloader

import io.buildfoundation.kaliy.cachelayer.CacheLayer
import io.buildfoundation.kaliy.config.Config

sealed class ModuleLoadResult {
    data class Ok(val cacheLayer: CacheLayer) : ModuleLoadResult()
    data class Error(val cause: Throwable) : ModuleLoadResult()
}

fun loadLayer(layer: Config.Layer): ModuleLoadResult {
    return try {
        val constructor = Class
                .forName(layer.`class`)
                .constructors
                .firstOrNull { it.parameters.size == 1 && it.parameterTypes[0].isAssignableFrom(Config.Layer::class.java) }

        if (constructor == null) {
            throw IllegalStateException("Class ${layer.`class`} does not have a constructor that takes ${Config.Layer::class.java.canonicalName}")
        }

        ModuleLoadResult.Ok(constructor.newInstance(layer) as CacheLayer)
    } catch (t: Throwable) {
        ModuleLoadResult.Error(t)
    }
}
