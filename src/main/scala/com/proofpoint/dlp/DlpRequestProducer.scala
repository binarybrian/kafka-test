package com.proofpoint.dlp

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.incidents.models.DlpRequest
import com.proofpoint.json.Json
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.tika.TikaExtract
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext.Implicits.global

class DlpRequestProducer(config: Config) extends KafkaMessageProducer(config) with Logging {
  private val dlpRequestTopic = config.getString("kafka.topic.dlp_request")

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $dlpRequestTopic")

  def sendRequest(dlpRequest: DlpRequest): Unit = {
    sendRequestJson(Json.toString(dlpRequest))
  }

  def sendRequestJson(dlpRequestJson: String): Unit = {
    sendMessage(dlpRequestTopic, dlpRequestJson)
  }
}


object DlpRequestProducerApp extends App {
  val startTime = System.currentTimeMillis()
  val inputStream = getClass.getResourceAsStream("/dlp_large.docx")
  val content = TikaExtract.extract(inputStream)
  println(s"Extract time: ${System.currentTimeMillis() - startTime} ms")
  inputStream.close()
}