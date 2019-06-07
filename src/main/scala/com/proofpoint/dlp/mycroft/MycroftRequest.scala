package com.proofpoint.dlp.mycroft

import play.api.libs.json.{Format, JsObject, Json}

case class MycroftRequest(context: RequestContext, requestDetails: JsObject, sendResponseTo: String, contentParts: Map[String, ContentPart])

object MycroftRequest {
  implicit val format: Format[MycroftRequest] = Json.format[MycroftRequest]
}