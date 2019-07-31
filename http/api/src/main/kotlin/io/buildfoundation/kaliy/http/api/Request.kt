package io.buildfoundation.kaliy.http.api

import okhttp3.Headers
import okhttp3.HttpUrl

class Request(val url: HttpUrl, val headers: Headers)
