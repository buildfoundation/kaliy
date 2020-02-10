package io.buildfoundation.kaliy.http.api

import okhttp3.Headers
import okhttp3.HttpUrl

// TODO add lazy body
data class HttpRequest(val url: HttpUrl, val method: HttpMethod, val headers: Headers)

enum class HttpMethod { GET, PUT, POST, DELETE, HEAD, PATCH }
