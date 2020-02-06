package io.buildfoundation.kaliy.http.api

import io.reactivex.Maybe

abstract class HttpHandler(protected val config: Map<String, Any>) {
    abstract fun handle(request: HttpRequest): Maybe<HttpResponse>
}
