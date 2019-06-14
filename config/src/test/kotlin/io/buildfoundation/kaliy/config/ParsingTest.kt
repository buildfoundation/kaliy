package io.buildfoundation.kaliy.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ParsingTest {

    @get:Rule
    val tmpDir = TemporaryFolder()

    @Test
    fun canParse() {
        val configFile = tmpDir.newFile()
        //language=JSON
        configFile.writeText("""
            {
              "http": {
                "port": 80
              },
              "batching": {
                "get": {
                  "windowMs": 1000,
                  "windowSize": 100
                },
                "put": {
                  "windowMs": 2000,
                  "windowSize": 200
                }
              },
              "layers": [
                {
                  "name": "memory",
                  "class": "com.memory.Memory",
                  "comment": "I am memory comment",
                  "timeoutMs": 5,
                  "config": {
                    "size": "64M"
                  }
                },
                {
                  "name": "S3",
                  "class": "com.aws.S3",
                  "comment": "I am S3 comment",
                  "timeoutMs": 1000,
                  "config": {
                    "bucket": "bucket",
                    "prefix": "abc",
                    "tokenEnvVarName": "S3_TOKEN"
                  }
                }
              ]
            }
        """.trimIndent()
        )

        assertThat(parseConfig(configFile)).isEqualTo(Config(
                http = Config.Http(port = 80),
                batching = Config.Batching(
                        get = Config.Batching.Get(windowMs = 1000, windowSize = 100),
                        put = Config.Batching.Put(windowMs = 2000, windowSize = 200)
                ),
                layers = listOf(
                        Config.Layer(
                                name = "memory",
                                `class` = "com.memory.Memory",
                                comment = "I am memory comment",
                                timeoutMs = 5,
                                config = mapOf(
                                        "size" to "64M"
                                )
                        ),
                        Config.Layer(
                                name = "S3",
                                `class` = "com.aws.S3",
                                comment = "I am S3 comment",
                                timeoutMs = 1000,
                                config = mapOf(
                                        "bucket" to "bucket",
                                        "prefix" to "abc",
                                        "tokenEnvVarName" to "S3_TOKEN"
                                )
                        ))
        ))
    }
}
