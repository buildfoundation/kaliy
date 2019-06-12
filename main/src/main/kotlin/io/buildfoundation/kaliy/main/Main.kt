@file:JvmName("Main")
package io.buildfoundation.kaliy.main

import io.buildfoundation.kaliy.config.parseConfig

internal fun main(rawArgs: Array<String>) {
    val args = parseArgs(rawArgs)
    val config = parseConfig(args.configFile)

    
}
