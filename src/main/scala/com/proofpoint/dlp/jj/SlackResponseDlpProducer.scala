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

class SlackResponseDlpProducer(config: Config) extends KafkaMessageProducer(config) with Logging {
  override val topic: String = config.getString("kafka.topic.dlp_response")

  def sendJsonMessage(jsonMessage: String): Unit = {
    sendMessage(topic, jsonMessage)
  }

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $topic")
}

object SlackResponseDlpProducerApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val dlpResponseProducer = new SlackResponseDlpProducer(config)

  println(s"Sending on topic ${dlpResponseProducer.topic}")

  dlpResponseProducer.sendJsonMessage(Source.fromResource("slack_dlp_response.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}