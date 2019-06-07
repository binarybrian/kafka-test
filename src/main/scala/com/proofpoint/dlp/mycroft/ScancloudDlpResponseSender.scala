package com.proofpoint
package dlp.mycroft

import play.api.libs.json.{JsObject, Json}

object ScancloudDlpResponseSenderApp extends App {
  val decompressed = decompress("H4sIAAAAAAAAAG2RSY/aQBBG/wriSjS0jTE4Ug5esIH21l6w8c0b3cZ4ARtvUf57mMwoikapqlMd6n169XMO3l49/z7fM82B/1vdcRQHj4fQvsq2XJuja1eeTR0qm1x6QFxbZhonxvXgkmRymae642TE+RnDZ4Pmd2L/fuTH/NvH+bc2HdqvDMPMcCWiqzZkgs3fR5ecbK027ydU2SGo3JiY7osEdxtNjcnxFBNdtdeGs1uemIWf8ViwOJwPOvqD+mA1z+iaxu8oh2TN7DUtSWef21l1mYWzJizqWzor0qYJcfqZkPoaTvKNZ8+K2isZE9C7ZCJDzBviuPOvgS9ZBJ1W8V3Yh/jYRmhRdypgC3PacMNCqxM9MQm1PYMShdGOB+tWMHtJYDxLwVUR94yc5dpQlKOTLrgrpW3jfP0g/hjcBkN4XGmS0qoIyGME62rtXbMhAOVeAqFScA8It/B+GB/RoUeij+06WiS66TYGDkyxKvg0L7LlsW1hB/lSckMsLQO+pKQEbRMPmWO65SSJSVc9DPr+H3EvBf99kuQZDXr3gHhujLY985SUyYZQeLi96/pyu9LBxNRiZFOX0Bk5+q7duK4jJrEMd7HJaEa6KJYeq/pdgGcfrwCPGilNEOLImdeDUAxWbgQVRfM2WC/rQ9RETzttkpyV2bXZqSpNpImSjz2F0doDUbRX77BkHXLMVjB4FMGtxBiI1SZng+2ipJvzBvGWFBgi7QxmBjxWvSVqr7GhUKiq8qycAirLPFzm3AQsBtPNqbPJxH+6+PUboYOvKxEDAAA=")
  println(decompressed)

  val decompressed2 = decompress("H4sIAAAAAAAAAAvJyCxWAKKSjFSFpPyUSoWS1IoShfw0hUSF4sTcgpxUhdzU4uLE9FQ9Xi4AixMXvCwAAAA=")
  println(decompressed2)

  val text = Json.toJson(Map("subject" -> "This is the subject", "body" -> "This is the body", "attachment" -> "This is the attachment")).toString

  println(text)

  val context = RequestContext("8fc6e791-64ad-49f9-8a11-d8d5903db83c")


  val blah = """{"subject":"This is the subject","body":"This is the body","attachment":"This is the attachment"}"""
  val compressedBlah = compress(blah)
  println(compressedBlah)
//  val request = MycroftRequest(
//    context,
//    JsObject.empty,
//    "https://scancloud-unified-dlp-prestaging.esp.lab.ppops.net:443/v1/results",

}
