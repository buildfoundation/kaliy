package io.buildfoundation.kaliy.config

import com.squareup.moshi.Json

data class Config(
        @Json(name = "http") val http: Http,
        @Json(name = "batching") val batching: Batching,
        @Json(name = "layers") val layers: List<Layer>
) {

    data class Http(
            @Json(name = "port") val port: Int,
            @Json(name = "handlers") val handlers: List<Handler>
    ) {
        data class Handler(
                @Json(name = "endpoint") val endpoint: String,
                @Json(name = "class") val `class`: String,
                @Json(name = "comment") val comment: String,
                @Json(name = "config") val config: Map<String, Any>
        )
    }

    data class Batching(@Json(name = "get") val get: Get,
                        @Json(name = "put") val put: Put
    ) {
        data class Get(
                @Json(name = "windowMs") val windowMs: Long,
                @Json(name = "windowSize") val windowSize: Int
        )

        data class Put(
                @Json(name = "windowMs") val windowMs: Long,
                @Json(name = "windowSize") val windowSize: Int
        )
    }

    data class Layer(
            @Json(name = "name") val name: String,
            @Json(name = "class") val `class`: String,
            @Json(name = "comment") val comment: String,
            @Json(name = "timeoutMs") val timeoutMs: Int,
            @Json(name = "config") val config: Map<String, Any>
    )


}
