package io.buildfoundation.kaliy.main

import java.io.File

internal data class Args(
        val configFile: File
)

internal fun parseArgs(rawArgs: Array<String>): Args {
    return Args()
}
