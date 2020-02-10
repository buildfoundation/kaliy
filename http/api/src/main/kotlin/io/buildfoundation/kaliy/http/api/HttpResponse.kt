package io.buildfoundation.kaliy.http.api

import okhttp3.Headers

// TODO add content
data class HttpResponse(
        val request: HttpRequest,
        val headers: Headers,
        val code: Int,
        val message: String = ""
)
