package com.proofpoint.dlp

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.json.Json
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.tika.TikaExtract
import com.typesafe.config.Config

import scala.concurrent.ExecutionContext.Implicits.global

class DlpRequestProducer(config: Config, dlpManager: DlpTestSuite) extends Logging {
  private val producer = new KafkaMessageProducer(config)
  private val dlpRequestTopic = config.getString("kafka.topic.dlp_request")

  logger.info(s"Sending on topic $dlpRequestTopic")

  def send(dlpRequest: DlpRequest): Unit = {
    producer.send(dlpRequestTopic, Json.toString(dlpRequest))
  }

  def close(): Unit = producer.close()
}


object DlpRequestProducerApp extends App {
  val startTime = System.currentTimeMillis()
  val inputStream = getClass.getResourceAsStream("/dlp_large.docx")
  val content = TikaExtract.extract(inputStream)
  println(s"Extract time: ${System.currentTimeMillis() - startTime} ms")
  inputStream.close()
}