package com.proofpoint
package dlp.jj

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.dlp.DlpResponseMatcher
import com.proofpoint.dlp.jj.FileDlpProducer.{filenamePrefix, numBytes}
import com.proofpoint.factory.{DlpDownload, WordOrNumberDocumentFactory}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.commons.json.Json._
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.s3.S3
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.Random
import scala.util.control.Breaks._

class FileDlpProducer(val config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher with Logging {
  private val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")

  def sendJsonMessage(jsonMessage: String): Unit = sendMessage(dlpDownloadCompleteTopic, jsonMessage)

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    println(s"Received dlpResponse with result ${dlpResponse.result} for logKey ${dlpResponse.logKey}")
  }

  def shutdown(): Unit = {
    super.close()
    super.stop()
  }

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $dlpDownloadCompleteTopic")
}

object FileDlpProducer {
  val numBytes = 4000000
  val filenamePrefix = "jj-test-%d"
}

/*
For 5 minutes:
- Randomly generate documents
- Upload to S3
- Post file to "dlp_download_complete" kafka topic
*/
object TimedLoadApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val fileProducer = new FileDlpProducer(config)

  val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")
  val dlpFeedProducer = new DlpFeedProducer(config)
  val documentFactory = new WordOrNumberDocumentFactory()
  val p = Promise[String]()

  println(s"Sending on topic '$dlpDownloadCompleteTopic'")
  val startTime = System.currentTimeMillis()
  while(true) {
    val index = Random.nextInt(10) + 1
    val filename = filenamePrefix.format(index)
    val s3Bucket = config.getString("s3.bucket.default")
    val dlpDownload = DlpDownload(s3Bucket, filename, numBytes)
    val dlpDownloadJson = dlpDownload.stringify

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
  fileProducer.close()
}

/*
Test text extraction load on JJ.  It doesn't test Sherlock dlpRequest or dlpResponse
Generate N number of events to kafka topic "dlp_download_complete"
See "DlpFileGenerateAndUploadApp" to generate the random document files.
 */
object FixedSizeLoadApp extends App {
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val fileProducer = new FileDlpProducer(config)

  val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")
  val s3Bucket = config.getString("s3.bucket.default")

  val numMessages = 20000

  println(s"Sending $numMessages on topic '$dlpDownloadCompleteTopic'")
  (0 to numMessages).foreach(i => {
    val index = Random.nextInt(10) + 1
    val filename = filenamePrefix.format(index)
    //val s3Path = s"https://s3.amazonaws.com/$s3BucketName/$filename"
    val dlpDownload = DlpDownload(s3Bucket, filename, numBytes)
    val dlpDownloadJson = dlpDownload.stringify

    fileProducer.sendJsonMessage(dlpDownloadJson)
  })

  fileProducer.close()
}

/*
For use with "FixedSizeLoadApp" to generate events on the "dlp_download_complete" kafka topic
- Generate 10 random document files
- Upload them to S3
 */
object DlpFileGenerateAndUploadApp extends App {
  val s3 = new S3()
  val documentFactory = new WordOrNumberDocumentFactory()
  val config = ConfigFactory.load()
  val s3Bucket = config.getString("s3.bucket.default")

  (1 to 10).foreach(i => {
    val filename = filenamePrefix.format(i)
    val document = documentFactory.makeDocumentOfSizeBytes(filename, numBytes)
    val zipPath = document.toZipFile
    Await.result(s3.upload(zipPath.toFile, s3Bucket, filename), Duration.Inf)
    val s3Path = s"https://s3.amazonaws.com/$s3Bucket/$filename"
    println(s"Finished uploading document of size ${document.words.size} bytes to $s3Path")
  })
}

object SingleFileDlpApp extends App {
  checkServiceStatusAll()

  val s3 = new S3()
  val bucketName = "flp-dlp-stg"
  val filename = "ed5745ee17514ad85b98dd138108405a"
  val tenantId = "tenant_10da1867d943489ebe6120ab0b6a1c26"
  val s3Path = s"s3://$bucketName/$filename"
  val numBytes = 71
  val dlpDownload = DlpDownload(bucketName, filename, 71, tenantId)

  val config = ConfigFactory.load()
  val topic = config.getString("kafka.topic.download_file_response")
  val fileProducer = new FileDlpProducer(config)

  println(s"Sending $s3Path to topic $topic for tenant ${dlpDownload.tenantId}")
  fileProducer.sendJsonMessage(dlpDownload.stringify)

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)

  fileProducer.shutdown()
  println("SingleFileDlpApp Finished.")
}