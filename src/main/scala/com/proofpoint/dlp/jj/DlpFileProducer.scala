package com.proofpoint.dlp.jj

import com.proofpoint.checkServiceStatus
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.dlp.DlpResponseMatcher
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class DlpFileProducer(config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher with Logging {
  private val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $dlpDownloadCompleteTopic")

  def sendJsonMessage(jsonMessage: String): Unit = sendMessage(dlpDownloadCompleteTopic, jsonMessage)

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"Received dlpResponse $dlpResponse")
  }
}

object DlpFileApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")
  println("Sending on topic 'dlp_download_complete'")

  val config = ConfigFactory.load()
  val fileProducer = new DlpFileProducer(config)

  fileProducer.sendJsonMessage(Source.fromResource("dlp_download_complete.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}