package com.proofpoint
package dlp.jj

import com.proofpoint.dlp.{DlpMessageProducer, DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.kafka.KafkaMessageProducer
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.io.Source

class EmailDlpProducer(val config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher {
  private val dlpRequestProducer = new DlpMessageProducer("kafka.topic.email", config)

  private val consumer = new DlpResponseConsumer(config, this)

  def sendRequest(jsonString: String): Unit = {
    dlpRequestProducer.sendRequestJson(jsonString)
  }

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"!!!! $dlpResponse")
  }

  def shutdown(): Unit = {
    dlpRequestProducer.close()
    consumer.stop()
  }
}

/*
Send a hard coded email attachment on kafka topic "cap_emailsetl_output"
A dlpResponse message should be returned from Sherlock.
 */
object EmailProducerApp extends App {
  val emailAttachmentOne = Source.fromResource("email_attach.json")
  val emailAttachmentTwo = Source.fromResource("email_attach_2.json")
  val emailAttachmentThree = Source.fromResource("email_attach_3.json")

  println("Sending email...")
  checkServiceStatusAll()

  val config = ConfigFactory.load()
  val producer = new EmailDlpProducer(config)

  producer.sendRequest(emailAttachmentThree.getLines().mkString(""))

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}
