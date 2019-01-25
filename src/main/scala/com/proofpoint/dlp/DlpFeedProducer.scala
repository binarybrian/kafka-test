package com.proofpoint.dlp

import com.proofpoint.checkServiceStatus
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class DlpFeedProducer(config: Config) extends Logging {
  private val producer = new KafkaMessageProducer(config)
  private val dlpFeedTopic = config.getString("kafka.topic.dlp_feed")

  logger.info(s"Sending on topic $dlpFeedTopic")

  def send(jsonString: String): Unit = {
    producer.send(dlpFeedTopic, jsonString)
  }

  def shutdown(): Unit = {
    producer.close()
  }
}

object DlpFeedApp extends App {
  println("Sending dlp_feed")
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val manager = new DlpFeedProducer(config)

  manager.send(Source.fromResource("share_level.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}