package com.proofpoint

object TestApp extends App {
  val decompressed = decompress("H4sIAAAAAAAAAKuuBQBDv6ajAgAAAA==")
  println(decompressed)

}
