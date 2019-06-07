package com.proofpoint.dlp.mycroft

import play.api.libs.json.{Format, Json}
import com.proofpoint.dlp.api.CustomerId

case class RequestContext(customerId: CustomerId)

object RequestContext {
  implicit val format: Format[RequestContext] = Json.format[RequestContext]
}