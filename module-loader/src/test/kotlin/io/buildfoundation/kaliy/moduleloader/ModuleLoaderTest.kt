package io.buildfoundation.kaliy.moduleloader

import io.buildfoundation.kaliy.cachelayer.CacheLayer
import io.buildfoundation.kaliy.config.Config
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ModuleLoaderTest {


    @Test
    fun `error when can't find constructor`() {
        class MyLayer : CacheLayer(Config.Layer("", "", "", 0, mapOf()))

        assertThat(loadLayer(Config.Layer(
                name = "my-layer",
                `class` = MyLayer::class.java.name,
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf("volodya" to "umerii")
        )).let { it as ModuleLoadResult.Error }.cause)
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessage("Class io.buildfoundation.kaliy.moduleloader.ModuleLoaderTest\$error when can't find constructor\$MyLayer does not have a constructor that takes io.buildfoundation.kaliy.config.Config.Layer")
    }

    @Test
    fun `error when class doesn't implements CacheLayer`() {
        data class MyLayer(val config: Config.Layer)

        assertThat(loadLayer(Config.Layer(
                name = "my-layer",
                `class` = MyLayer::class.java.name,
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf()
        )).let { it as ModuleLoadResult.Error }.cause)
                .isInstanceOf(ClassCastException::class.java)
                .hasMessage("io.buildfoundation.kaliy.moduleloader.ModuleLoaderTest\$error when class doesn't implements CacheLayer\$MyLayer cannot be cast to io.buildfoundation.kaliy.cachelayer.CacheLayer")
    }

    @Test
    fun `it works`() {
        class MyLayer(val myConfig: Config.Layer) : CacheLayer(myConfig)

        val config = Config.Layer(
                name = "my-layer",
                `class` = MyLayer::class.java.name,
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf()
        )

        val layer = (loadLayer(config) as ModuleLoadResult.Ok).cacheLayer as MyLayer

        assertThat(layer.myConfig).isEqualTo(config)
    }

    @Test
    fun `error when cache-layer throws in constructor`() {
        class MyLayer(config: Config.Layer) : CacheLayer(config) {
            init {
                throw RuntimeException("???")
            }
        }

        assertThat(loadLayer(Config.Layer(
                name = "my-layer",
                `class` = MyLayer::class.java.name,
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf()
        )).let { it as ModuleLoadResult.Error }.cause.cause)
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("???")
    }

    @Test
    fun `error when class not found`() {
        assertThat(loadLayer(Config.Layer(
                name = "my-layer",
                `class` = "???",
                comment = "My layer",
                timeoutMs = 1000,
                config = mapOf()
        )).let { it as ModuleLoadResult.Error }.cause)
                .isInstanceOf(ClassNotFoundException::class.java)
                .hasMessage("???")
    }
}
