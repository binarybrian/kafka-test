package com.proofpoint

import scala.sys.process._

object VaultReader {
  val resource = "sherlock.conf"
  private val regex = "export ([^=]+)='([^']+)'".r
  def readVaultProperties(env: String): Unit = {
    println(s"Loading $resource variables from $env vault...")
    val result = s"../do-vault/do-vault.sh --entity configfile --conf ../do-vault/config/$env.conf --files src/main/resources/$resource".!!
    regex.findAllIn(result).matchData.foreach {
      m =>
        val key = m.group(1)
        val value = m.group(2)
        println(s"$key=$value")
        System.setProperty(key, value)
    }
  }
}

object VaultReaderApp extends App {
  VaultReader.readVaultProperties("dev")
}