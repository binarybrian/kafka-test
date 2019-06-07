package com.proofpoint
package dlp.mycroft

import com.proofpoint.dlp.DlpMessageProducer
import com.typesafe.config.ConfigFactory

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.io.Source

object ScancloudDlpResponseProducerApp extends App{
  val config = ConfigFactory.load()
  val scancloudResponse = Source.fromResource("scancloud-response2.json")
  val producer = new DlpMessageProducer("kafka.topic.dlp.scancloud_response", config)
  checkServiceStatus("mycroft", "http://localhost:9000")

  producer.sendRequestJson(scancloudResponse.getLines().mkString(""))
  println("Sent reponse to mycroft!!!")

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}
