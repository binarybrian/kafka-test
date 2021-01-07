package com.proofpoint.dlp

import awscala.Resource
import com.google.common.io.BaseEncoding
import com.proofpoint.commons.efs.EfsService
import com.proofpoint.commons.logging.NoLoggingContext
import com.proofpoint.commons.metrics.fake.FakeMetricService
import com.proofpoint.dlp.EncodeApp.encoded
import com.proofpoint.dlp.api.DetectorId
import com.proofpoint.incidents.models.ExtractedContentPart
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import scala.collection.mutable.ListBuffer

//object DecodeApp extends App {
//  //export PRIVATELYSHARED_DATATRANSFER_DECRYPTIONKEY='1:AeJcBOB/Z7KKzrAcXzDjVzY='
//  //export PRIVATELYSHARED_DATATRANSFER_ENCRYPTIONKEY='1:AeJcBOB/Z7KKzrAcXzDjVzY='
//  System.setProperty("PRIVATELYSHARED_DATATRANSFER_DECRYPTIONKEY", "1:AeJcBOB/Z7KKzrAcXzDjVzY=")
//  System.setProperty("PRIVATELYSHARED_DATATRANSFER_ENCRYPTIONKEY", "1:AeJcBOB/Z7KKzrAcXzDjVzY=")
//  val config = ConfigFactory.load()
//  val metricService = FakeMetricService
//  val efsService = new EfsService(config, metricService)
//
//  //val url = """data:application/vnd.pfpt.smart-encoded;base64,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"""
//  val url = "temp.base64.pfpt"
//  val response = efsService.readUrl(url)(NoLoggingContext)
//  Files.write(Paths.get("/Users/bmerrill/tenant-y-content.txt"), response.getBytes(StandardCharsets.UTF_8))
//  //println(response)
//}

object DecodeApp extends App {
  //export PRIVATELYSHARED_DATATRANSFER_DECRYPTIONKEY='1:AeJcBOB/Z7KKzrAcXzDjVzY='
  //export PRIVATELYSHARED_DATATRANSFER_ENCRYPTIONKEY='1:AeJcBOB/Z7KKzrAcXzDjVzY='

  System.setProperty("efs.path", "/tmp")
  Json.parse(Files.readAllBytes(Paths.get(s"${System.getProperty("user.dir")}/eu2-keys.json"))).as[Map[String, String]].foreach {
    case (key, value) => System.setProperty(key, value)
  }

  val config = ConfigFactory.load()
  val metricService = FakeMetricService
  val efsService = new EfsService(config, metricService)

  val url = """data:application/vnd.pfpt.smart-encoded;base64,AAAAAAAAAAAAAAACH4sIAAAAAAAAAKtWKkotLs0pKVayqq6tBQBIRbUzDgAAAA=="""
  val response = efsService.readUrl(url)(NoLoggingContext)
  //Files.write(Paths.get("/Users/bmerrill/tenant-y-content.txt"), response.getBytes(StandardCharsets.UTF_8))
  println(response)
  //Files.write(Paths.get(s"${System.getProperty("user.dir")}/response.txt"), response.getBytes(StandardCharsets.UTF_8))
  //val encoded = BaseEncoding.base64().encode(response.getBytes)
  //val encodedContext = s"data:;base64,$encoded"
  //println(encodedContext)
  //Files.write(Paths.get(s"${System.getProperty("user.dir")}/response-encoded.txt"), encodedContext.getBytes(StandardCharsets.UTF_8))
}

object EncodeApp extends App {
  val filePath = Paths.get("/testpool/pfpt/response.txt")
  val bytes = Files.readAllBytes(filePath)
  val encoded = BaseEncoding.base64().encode("""{"results":{}}""".getBytes)
  val content = ExtractedContentPart(s"data:;base64,$encoded")
  println(content)
}

//object ParseDetectorsApp extends App {
//  import play.api.libs.json.{JsObject, Json}
//
//  val detectors = Json.parse(Files.newInputStream(Paths.get("/Users/bmerrill/booking-com-detectors.json"))).as[Map[DetectorId, DetectorInfo]]
//  val smartIdBuffer = new ListBuffer[String]
//  detectors.foreach {
//    case (detectorName, detectorInfo) =>
//      //val smartIdName = detectorInfo.smartIds.keys.toSet.mkString(",")
//      //val smartIdInfo = s"$detectorName\n\t$smartIdName"
//      smartIdBuffer.appendAll(detectorInfo.smartIds.keys.toSet)
//    case _ => ""
//  }
//  val allSmartIds = smartIdBuffer.toSet
//  val metadataResult = allSmartIds.mkString(",")
//  Files.write(Paths.get(s"/Users/bmerrill/booking-com-smartids-${allSmartIds.size}.txt"), metadataResult.getBytes(StandardCharsets.UTF_8))
//  //val id = alljson \\ "_id"
//  //val filter: String = id.filter(_.as[String].startsWith("Sherlock")).mkString(",\n")
//  //println(detectors)
//}
