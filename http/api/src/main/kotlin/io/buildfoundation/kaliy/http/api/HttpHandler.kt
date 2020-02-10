package io.buildfoundation.kaliy.http.api

import io.buildfoundation.kaliy.config.Config
import io.reactivex.Maybe

abstract class HttpHandler(protected val config: Config.Http.Handler) {
    abstract fun handle(request: HttpRequest): Maybe<HttpResponse>
}
