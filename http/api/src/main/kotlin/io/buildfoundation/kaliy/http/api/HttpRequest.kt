package io.buildfoundation.kaliy.http.api

import okhttp3.Headers
import okhttp3.HttpUrl

data class HttpRequest(val url: HttpUrl, val headers: Headers, val method: HttpMethod)

enum class HttpMethod { GET, PUT, POST, DELETE, HEAD, PATCH }
