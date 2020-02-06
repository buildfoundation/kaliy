package io.buildfoundation.kaliy.handlers.gradle

import io.buildfoundation.kaliy.http.api.HttpHandler
import io.buildfoundation.kaliy.http.api.HttpMethod
import io.buildfoundation.kaliy.http.api.HttpRequest
import io.buildfoundation.kaliy.http.api.HttpResponse
import io.reactivex.Maybe
import okhttp3.Headers

class GradleHttpHandler(config: Map<String, Any>) : HttpHandler(config) {

    override fun handle(request: HttpRequest): Maybe<HttpResponse> = Maybe.create {
        when (request.method) {
            HttpMethod.GET -> HttpResponse(request, Headers.of(), 404)
            HttpMethod.PUT -> HttpResponse(request, Headers.of(), 202)
            else -> HttpResponse(request, Headers.of(), 405, "GradleHttpHandler got unsupported method ${request.method}. Only GET and PUT are supported.")
        }
    }

}
