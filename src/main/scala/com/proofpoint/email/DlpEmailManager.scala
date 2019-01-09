package com.proofpoint.email

import com.proofpoint.checkServiceStatus
import com.proofpoint.dlp.{DlpRequestProducer, DlpResponse, DlpResponseConsumer, DlpResponseMatcher}
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class DlpEmailManager(config: Config) extends DlpResponseMatcher {
  private val producer = new DlpRequestProducer(config)
  private val consumer = new DlpResponseConsumer(config, this)

  def send(jsonString: String): Unit = {
    producer.sendJson(jsonString)
  }

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"$dlpResponse")
  }

  def shutdown(): Unit = {
    producer.close()
    consumer.stop()
  }
}

object EmailSenderApp extends App {
  println("Sending email...")
  checkServiceStatus("jessica-jones", "http://localhost:9000")
  checkServiceStatus("sherlock", "http://localhost:9001")

  val config = ConfigFactory.load()
  val manager = new DlpEmailManager(config)

  manager.send(Source.fromResource("email_attach.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}
