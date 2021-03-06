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
  override val topic: String = config.getString("kafka.topic.dlp_feed")

  def sendJsonMessage(jsonMessage: String): Unit = {
    sendMessage(topic, jsonMessage)
  }

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $topic")
}

/*
-- Send a DlpScanRequest to test the Stanford hard coded share level.  The share level can be changed
in share_level.json to see (via logs) if the scan request is skipped or processed accordingly.

-- This is also a test of the dlp_feed consumer.
 */
object DlpShareLevelApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  println("Sending on topic dlp_feed")

  val config = ConfigFactory.load()
  val dlpFeedProducer = new DlpFeedProducer(config)

  dlpFeedProducer.sendJsonMessage(Source.fromResource("share_level.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}