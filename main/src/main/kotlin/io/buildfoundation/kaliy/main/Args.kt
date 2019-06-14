package io.buildfoundation.kaliy.main

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import java.io.File

internal data class Args(
        val configFile: File
)


internal fun parseArgs(rawArgs: Array<String>): Args {
    class Parser(parser: ArgParser) {
        val config by parser.storing(help = "Path to the config file", names = *arrayOf("--config")) { File(this) }
    }

    try {
        val parser = Parser(ArgParser(rawArgs))
        return Args(parser.config)
    } catch (e: SystemExitException) {
        e.printAndExit()
    }
}
