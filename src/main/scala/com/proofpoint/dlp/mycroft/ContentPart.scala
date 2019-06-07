package com.proofpoint
package dlp.mycroft

import play.api.libs.json.{Format, Json}

case class ContentPart(content: String, encoding: Option[String] = None) {
  def asString: String = {
    if (encoding.contains("GZIP/BASE64")) decompress(content)
    else content
  }
}

object ContentPart {
  implicit val format: Format[ContentPart] = Json.format[ContentPart]
}
