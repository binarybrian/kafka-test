package com.proofpoint.dlp.jj

import com.proofpoint.checkServiceStatus
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.dlp.{DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.{Await, Promise}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source

class ScanDlpProducer(config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher with Logging {
  private val consumer = new DlpResponseConsumer(config, this)

  override val topic: String = config.getString("kafka.topic.dlp_stream")

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"DlpResponse: $dlpResponse")
  }

  override def shutdown(): Unit = {
    consumer.shutdown()
    super.shutdown()
  }

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $topic")

  consumer.start()
}

object StreamDlpProducerApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")
  //checkServiceStatus("sherlock", "http://localhost:9001")

  val config = ConfigFactory.load()
  val producer = new ScanDlpProducer(config)

  println(s"Sending on topic ${producer.topic}")

  val message = Source.fromResource("dlp_stream.json").getLines().mkString("")
  producer.sendMessage(message)

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}