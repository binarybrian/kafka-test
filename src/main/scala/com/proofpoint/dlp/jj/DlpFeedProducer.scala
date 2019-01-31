package com.proofpoint.dlp.jj

import com.proofpoint.checkServiceStatus
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class DlpFeedProducer(config: Config) extends KafkaMessageProducer(config) with Logging {
  private val dlpFeedTopic = config.getString("kafka.topic.dlp_feed")

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $dlpFeedTopic")

  def sendJsonMessage(jsonMessage: String): Unit = {
    sendMessage(dlpFeedTopic, jsonMessage)
  }
}

object DlpFeedApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")
  println("Sending on topic dlp_feed")

  val config = ConfigFactory.load()
  val dlpFeedProducer = new DlpFeedProducer(config)

  dlpFeedProducer.sendJsonMessage(Source.fromResource("share_level.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}