package com.proofpoint

import scala.sys.process._

object VaultReader {
  private val regex = "export ([^=]+)='([^']+)'".r
  def readVaultProperties(env: String): Unit = {
    println("loading variables from vault...")
    val result = s"../do-vault/do-vault.sh --entity configfile --conf ../do-vault/config/$env.conf --files src/main/resources/service.conf".!!
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