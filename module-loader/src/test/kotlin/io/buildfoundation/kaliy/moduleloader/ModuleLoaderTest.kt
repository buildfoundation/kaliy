package io.buildfoundation.kaliy.moduleloader

import io.buildfoundation.kaliy.cachelayer.CacheLayer
import io.buildfoundation.kaliy.config.Config
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ModuleLoaderTest {

    class MyLayerWithoutConstructor : CacheLayer

    @Test
    fun name() {
        assertThat(loadLayer(Config.Layer(
                name = "my-layer",
                `class` = MyLayerWithoutConstructor::class.java.name,
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf("volodya" to "umerii")
        )).let { it as ModuleLoadResult.Error }.cause)
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Class io.buildfoundation.kaliy.moduleloader.ModuleLoaderTest\$MyLayerWithoutConstructor does not have a constructor that takes io.buildfoundation.kaliy.config.Config.Layer")
    }
}
