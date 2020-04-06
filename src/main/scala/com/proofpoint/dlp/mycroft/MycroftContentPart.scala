package com.proofpoint.dlp.mycroft

import com.proofpoint.commons.util.RawTranscoder.decode
import com.proofpoint.commons.util.compression.GZipCompressor
import play.api.libs.json.{Format, Json}
import com.proofpoint.dlp.api.ContentPart

case class MycroftContentPart(content: String, encoding: Option[String] = None, `type`: Option[String] = None) {
  def toContentPart: ContentPart = ContentPart(decodedContent, `type`)

  private def decodedContent: String = {
    if (encoding.contains("GZIP/BASE64")) decode(content, Some(GZipCompressor))
    else if (encoding.contains("UTF8")) content
    else if (encoding.nonEmpty) throw new Exception(s"Unknown encoding $encoding")
    else content
  }
}

object MycroftContentPart {
  implicit val format: Format[MycroftContentPart] = Json.format[MycroftContentPart]
}