package com.proofpoint
package dlp.jj

import com.proofpoint.dlp.{DlpRequestProducer, DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class DlpEmailProducer(val config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher {
  private val dlpRequestProducer = new DlpRequestProducer(config)
  private val consumer = new DlpResponseConsumer(config, this)

  def sendRequest(jsonString: String): Unit = {
    dlpRequestProducer.sendRequestJson(jsonString)
  }

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"$dlpResponse")
  }

  def shutdown(): Unit = {
    dlpRequestProducer.close()
    consumer.stop()
  }
}

/*
Send a hard code email attachment on kafka topic "cap_emailsetl_output"
A dlpResponse message should be returned from Sherlock.
 */
object EmailSendApp extends App {
  println("Sending email...")
  checkServiceStatusAll()

  val config = ConfigFactory.load()
  val producer = new DlpEmailProducer(config)

  producer.sendRequest(Source.fromResource("email_attach.json").getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}