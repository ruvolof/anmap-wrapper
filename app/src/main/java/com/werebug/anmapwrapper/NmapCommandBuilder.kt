package com.werebug.anmapwrapper

import java.util.Collections

class NmapCommandBuilder(
  private val nmapExecutablePath: String,
  private val dataDirPath: String,
  private val xmlOutputPath: String?,
  private val defaultDnsServers: String = "8.8.8.8",
) {

  enum class ErrorKind { INVALID_SUDO_SYNTAX, INVALID_NMAP_SYNTAX, RESERVED_FLAG }

  sealed class Result {
    data class Success(val argv: List<String>) : Result()
    data class Error(val kind: ErrorKind, val flag: String? = null) : Result()
  }

  fun build(rawCommand: String): Result {
    val argv = rawCommand.trim().split(Regex("\\s+")).toMutableList()

    val sudoIndex = argv.indexOf("sudo")
    if (sudoIndex > 0) {
      return Result.Error(ErrorKind.INVALID_SUDO_SYNTAX)
    }
    if (sudoIndex == 0) {
      argv.removeAt(0)
      argv.addAll(0, listOf("su", "-c"))
    }

    val nmapIndex = argv.indexOf("nmap")
    if (sudoIndex == -1 && nmapIndex != 0 || sudoIndex == 0 && nmapIndex != 2) {
      return Result.Error(ErrorKind.INVALID_NMAP_SYNTAX)
    }
    argv[nmapIndex] = nmapExecutablePath

    if (containsFlag(argv, "--datadir")) {
      return Result.Error(ErrorKind.RESERVED_FLAG, "--datadir")
    }
    Collections.addAll(argv, "--datadir", dataDirPath)

    if (!containsFlag(argv, "--dns-servers")) {
      Collections.addAll(argv, "--dns-servers", defaultDnsServers)
    }

    if (xmlOutputPath != null) {
      if (containsFlag(argv, "-oX")) {
        return Result.Error(ErrorKind.RESERVED_FLAG, "-oX")
      }
      Collections.addAll(argv, "-oX", xmlOutputPath)
    }

    return Result.Success(argv)
  }

  private fun containsFlag(argv: List<String>, flag: String): Boolean {
    val prefix = "$flag="
    return argv.any { it == flag || it.startsWith(prefix) }
  }
}
