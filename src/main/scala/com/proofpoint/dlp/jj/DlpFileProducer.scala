package com.proofpoint
package dlp.jj

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.dlp.jj.DlpFileProducer.{filenamePrefix, numBytes}
import com.proofpoint.dlp.{DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.factory.{DlpDocument, DlpDownload, WordOrNumberDocumentFactory}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.json.Json
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.s3.S3
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.Random
import scala.util.control.Breaks._

class DlpFileProducer(config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher with Logging {
  private val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $dlpDownloadCompleteTopic")

  def sendJsonMessage(jsonMessage: String): Unit = sendMessage(dlpDownloadCompleteTopic, jsonMessage)

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"Received dlpResponse $dlpResponse")
  }
}

object DlpFileProducer {
  val numBytes = 4000000
  val filenamePrefix = "jj-test-%d"
}

object TimedLoadApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val fileProducer = new DlpFileProducer(config)

  val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")
  val dlpFeedProducer = new DlpFeedProducer(config)
  val documentFactory = new WordOrNumberDocumentFactory()
  val p = Promise[String]()

  println(s"Sending on topic '$dlpDownloadCompleteTopic'")
  val startTime = System.currentTimeMillis()
  while(true) {
    val index = Random.nextInt(10) + 1
    val filename = filenamePrefix.format(index)
    val s3Path = s"https://s3.amazonaws.com/$s3BucketName/$filename"
    val dlpDownload = DlpDownload(filename, s3Path, numBytes)
    val dlpDownloadJson = Json.toString(dlpDownload)

    fileProducer.sendJsonMessage(dlpDownloadJson)
    if (System.currentTimeMillis() - startTime > 300000) {
      p.success("Finished")
      break
    }
    else {
      Thread.sleep(1000L)
    }
  }

  val f = p.future
  val result = Await.result(f, Duration.Inf)
  println(result)
}

object FixedSizeLoadApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val fileProducer = new DlpFileProducer(config)

  val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")
  val dlpFeedProducer = new DlpFeedProducer(config)
  val documentFactory = new WordOrNumberDocumentFactory()
  val p = Promise[String]()

  val numMessages = 20000

  println(s"Sending $numMessages on topic '$dlpDownloadCompleteTopic'")
  (0 to numMessages).foreach(i => {
    val index = Random.nextInt(10) + 1
    val filename = filenamePrefix.format(index)
    val s3Path = s"https://s3.amazonaws.com/$s3BucketName/$filename"
    val dlpDownload = DlpDownload(filename, s3Path, numBytes)
    val dlpDownloadJson = Json.toString(dlpDownload)

    fileProducer.sendJsonMessage(dlpDownloadJson)
  })
}

object DlpFileGenerateAndUploadApp extends App {
  val s3 = new S3()
  val documentFactory = new WordOrNumberDocumentFactory()

  (1 to 10).foreach(i => {
    val filename = filenamePrefix.format(i)
    val document = documentFactory.makeDocumentOfSizeBytes(filename, numBytes)
    val zipPath = document.toZipFile
    Await.result(s3.upload(zipPath.toFile, s3BucketName, filename), Duration.Inf)
    val s3Path = s"https://s3.amazonaws.com/$s3BucketName/$filename"
    println(s"Finished uploading document of size ${document.words.size} bytes to $s3Path")
  })
}