package com.proofpoint.dlp.sherlock

import com.proofpoint.commons.json.Implicits._
import com.proofpoint.commons.logging.SimpleLoggingContext
import com.proofpoint.commons.util.SmartTranscoder
import com.proofpoint.commons.util.compression.CompressionAlgorithm
import com.proofpoint.commons.util.compression.CompressionAlgorithm.GZIP
import com.proofpoint.compress
import com.proofpoint.incidents.models.DlpRequest
import play.api.libs.json.JsObject

object makeDlpRequest {
//  def encodeContent(contentMap: Map[String, String]): Either[String, String] = {
//    val encodedContent = SmartTranscoder.encode(contentMap.stringify, GZIP, Some("datatransfer"))
//    if (encodedContent.length < minEfsContentSize) {
//      metricService.increment(METRIC_KAFKA_FILE_COUNT)
//      Left(encodedContent)
//    }
//    else {
//      metricService.increment(METRIC_EFS_FILE_COUNT)
//      Right(writeEfsFile(encodedContent))
//    }
//  }
}

object MakeDlpRequestApp extends App {
  val content = Map("text" -> "ssn 122-33-4444")
  //val dlpRequest = DlpRequest("logkey123", "tenantid123", Some(SmartTranscoder.encode(content.stringify, CompressionAlgorithm.GZIP, None)), None, JsObject.empty)
  //println(dlpRequest.stringify)
}
