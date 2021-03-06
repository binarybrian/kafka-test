package com.proofpoint.dlp

import com.proofpoint.commons.json.Implicits._
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.incidents.models.DlpRequest
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.tika.TikaExtract
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext.Implicits.global

class DlpMessageProducer(topicPath: String, config: Config) extends KafkaMessageProducer(config) with Logging {
  override val topic: String = config.getString(topicPath)

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $topic")

  def sendRequest(dlpRequest: DlpRequest): Unit = {
    sendRequestJson(dlpRequest.stringify)
  }

  def sendRequestJson(dlpMessageJson: String): Unit = {
    sendMessage(topic, dlpMessageJson)
  }
}


object DlpRequestProducerApp extends App {
  val startTime = System.currentTimeMillis()
  val inputStream = getClass.getResourceAsStream("/dlp_large.docx")
  val content = TikaExtract.extract(inputStream)
  println(s"Extract time: ${System.currentTimeMillis() - startTime} ms")
  inputStream.close()
}

object HackdaysApp extends App {
  val startTime = System.currentTimeMillis()
  val inputStream = getClass.getResourceAsStream("/embedded-image-dlp.pdf")
  val content = TikaExtract.extract(inputStream)
  println(s"Extracted: $content")
  println(s"Extract time: ${System.currentTimeMillis() - startTime} ms")
  inputStream.close()
}