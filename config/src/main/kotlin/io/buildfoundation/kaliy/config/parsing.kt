package io.buildfoundation.kaliy.config

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okio.Okio
import java.io.File

fun parseConfig(file: File): Config = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(Config::class.java)
        .nonNull()
        .fromJson(Okio.buffer(Okio.source(file)))!!
