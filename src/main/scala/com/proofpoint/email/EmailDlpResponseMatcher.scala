package com.proofpoint.email

import com.proofpoint.checkServiceStatus
import com.proofpoint.dlp.{DlpRequestProducer, DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.incidents.models.DlpResponse
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class EmailDlpResponseMatcher(config: Config) extends DlpResponseMatcher {
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

object EmailSendApp extends App {
  println("Sending email...")
  checkServiceStatus("jessica-jones", "http://localhost:9000")
  checkServiceStatus("watson", "http://localhost:9001")
  checkServiceStatus("sherlock", "http://localhost:9002") //Sherlock hard codes Watson to localhost:9001 for testing.

  val config = ConfigFactory.load()
  val manager = new EmailDlpResponseMatcher(config)

  manager.send(Source.fromResource("email_attach.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}