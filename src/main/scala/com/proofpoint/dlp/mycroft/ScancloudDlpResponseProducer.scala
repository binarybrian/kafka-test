package com.proofpoint
package dlp.mycroft

import com.proofpoint.commons.json.Implicits._
import com.proofpoint.dlp.DlpMessageProducer
import com.typesafe.config.ConfigFactory
import play.api.libs.json.{JsError, JsObject, JsSuccess}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
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


object MycroftRequestApp extends App {

  val blahString = "ABSAASDFASDFASDFASDFASDF"
  println(s"!!!!!! ${blahString.replace("Bearer ", "")}")

  val request = Source.fromResource("mycroft_request.json").mkString.normalize
  val mycroftRequest = request.asOpt[JsObject] match {
    case Some(jsObject) => jsObject.validate[MycroftRequest] match {
      case JsSuccess(jsRequest, _) => jsRequest
      case JsError(errors) => throw new Exception(errors.mkString(","))
    }
    case None => throw new Exception(s"Request is not a JsObject: $request")
  }
  val logKey = s"${(mycroftRequest.requestDetails \ "customer").as[String]} ${(mycroftRequest.requestDetails \ "job_id").as[String]}"
  val logKey2 = Seq((mycroftRequest.requestDetails \ "customer").asOpt[String], (mycroftRequest.requestDetails \ "job_id").asOpt[String]).flatten.mkString(" ")
  println(s"logKey: $logKey")
  println(s"logKey2: $logKey2")
}