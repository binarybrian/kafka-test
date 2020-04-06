package com.proofpoint
package dlp.jj

import java.nio.file.Paths
import java.time
import java.util.concurrent.TimeUnit

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.dlp.{DlpResponseConsumer, DlpResponseMatcher}
import com.proofpoint.dlp.jj.FileDlpProducer.{filenamePrefix, numBytes}
import com.proofpoint.factory.{DlpDownload, WordOrNumberDocumentFactory}
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.kafka.KafkaMessageProducer
import com.proofpoint.s3.S3
import com.typesafe.config.{Config, ConfigFactory}
import com.proofpoint.commons.json.Implicits._
import com.proofpoint.commons.util.duration.DurationUtils
import com.proofpoint.dlp.jj.DlpFileGenerateAndUploadApp.s3Bucket

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, Promise}
import scala.util.Random
import scala.util.control.Breaks._

class FileDlpProducer(val config: Config) extends KafkaMessageProducer(config) with DlpResponseMatcher with Logging {
  private val consumer = new DlpResponseConsumer(config, this)

  override val topic: String = config.getString("kafka.topic.download_file_response")

  override def matchResponse(dlpResponse: DlpResponse): Unit = {
    //println(s"Received dlpResponse with result ${dlpResponse.result} for logKey ${dlpResponse.request.requestId}")
  }

  override def shutdown(): Unit = {
    consumer.shutdown()
    super.shutdown()
  }

  logger.info(s"Starting producer ${getClass.getSimpleName} on topic $topic")
  consumer.start()
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
  val promise = Promise[String]()

  println(s"Sending on topic '$dlpDownloadCompleteTopic'")
  val startTime = System.currentTimeMillis()
  while(true) {
    val index = Random.nextInt(10) + 1
    val filename = filenamePrefix.format(index)
    val s3Bucket = config.getString("s3.bucket.default")
    val dlpDownload = DlpDownload(s3Bucket, filename, numBytes)
    val dlpDownloadJson = dlpDownload.stringify

    fileProducer.sendMessage(dlpDownloadJson)
    if (System.currentTimeMillis() - startTime > 300000) {
      promise.success("Finished")
      break
    }
    else {
      Thread.sleep(1000L)
    }
  }

  val f = promise.future
  val result = Await.result(f, Duration.Inf)
  println(result)
  fileProducer.shutdown()
}

/*
Test text extraction load on JJ.  It doesn't test Sherlock dlpRequest or dlpResponse
Generate N number of events to kafka topic "dlp_download_complete"

See "DlpFileGenerateAndUploadApp" to generate the random document files.

Change "numMessages" to generate "N" number of events
 */
object FixedSizeLoadApp extends App {
  import com.proofpoint.commons.json.Implicits._
  checkServiceStatus("jessica-jones", "http://localhost:9000")

  val config = ConfigFactory.load()
  val fileProducer = new FileDlpProducer(config)

  val dlpDownloadCompleteTopic = config.getString("kafka.topic.download_file_response")
  val s3Bucket = config.getString("s3.bucket.default")

  val numMessages = 1

  println(s"Sending $numMessages message(s) on topic '$dlpDownloadCompleteTopic'")
  (1 to numMessages).foreach(i => {
    val index = Random.nextInt(2) + 1
    val filename = filenamePrefix.format(index)
    val dlpDownload = DlpDownload(s3Bucket, filename, numBytes)
    val dlpDownloadJson = dlpDownload.stringify

    fileProducer.sendMessage(dlpDownloadJson)
    if (i % 10 == 0) println (s"Sent $i of $numMessages messages")
  })

  fileProducer.shutdown()
  System.exit(0)
}

/*
For use with "FixedSizeLoadApp" to generate events on the "dlp_download_complete" kafka topic
- Generate 10 random document files
- Compress to zip files
- Upload to S3
 */
object DlpFileGenerateAndUploadApp extends App {
  val s3 = new S3()
  val documentFactory = new WordOrNumberDocumentFactory()
  val config = ConfigFactory.load()
  val s3Bucket = config.getString("s3.bucket.default")
  val fileType = FileType.Pdf

  (1 to 10).foreach(i => {
    val filename = filenamePrefix.format(i)
    val document = documentFactory.makeDocumentOfSizeBytes(filename, numBytes)
    val filePath = fileType match {
      case FileType.Zip => document.toZipFile
      case FileType.Plain =>  document.toFile(true)
      case FileType.Pdf => document.toPdfBox
    }
    val s3Path = s"https://s3.amazonaws.com/$s3Bucket/$filename"
    println(s"Uploading $filePath to $s3Path")
    Await.result(s3.upload(filePath.toFile, s3Bucket, filename), Duration.Inf)
    println(s"Finished uploading document of size ${document.words.size} bytes to $s3Path")
  })
}

object FileType extends Enumeration {
  val Plain, Zip, Pdf = Value
}

/*
Send a single file on the "dlp_download_complete" topic
 */
object SingleFileDlpApp extends App {
  //checkServiceStatusAll()
  val jjUrlLocal = "http://localhost:9000"
  val jjUrlDev = "https://jessica-jones.idev.infoprtct.com:9443"

  val dlpBucketTest = "infoprtct-watson-dev"
  val dlpBucketDev = "flp-dlp-dev"

  val dlpTestSmall = "dlp_small.docx"
  val dlpTestLarge = "test2.docx"

  checkServiceStatus("jessica-jones", jjUrlLocal)

  val s3 = new S3()

  val s3Bucket = dlpBucketTest
  val filename = dlpTestSmall

  val filePath = s"/Users/bmerrill/workspace/pfpt/$filename"
  val s3uploadPath = s"https://s3.amazonaws.com/$s3Bucket/$filename"

  println(s"Uploading $filePath to $s3uploadPath")
  Await.result(s3.upload(Paths.get(filePath).toFile, s3Bucket, filename), Duration.Inf)
  println(s"Finished uploading document to $s3uploadPath")

  //val bucketName = "infoprtct-watson-dev"

  val tenantId = "tenant_a9998b6b7083490784afda48dd383928"

  val numBytes = 26522
  val dlpDownload = DlpDownload(s3Bucket, filename, numBytes, tenantId)

  val config = ConfigFactory.load()
  val topic = config.getString("kafka.topic.download_file_response")
  val fileProducer = new FileDlpProducer(config)

  val s3Path = s"s3://$s3Bucket/$filename"
  println(s"Sending $s3Path to topic $topic for tenant ${dlpDownload.tenantId}")
  fileProducer.sendMessage(dlpDownload.stringify)

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)

  fileProducer.shutdown()
  println("SingleFileDlpApp Finished.")
}

object ConfigTestApp extends App {
  import java.time.{Duration => JavaDuration}
  val config = ConfigFactory.load()

  val timeout: JavaDuration = config.getDuration("authenticate.timeout")
  val authenticateTimeout: FiniteDuration = DurationUtils.toFiniteDuration(timeout)
  println(Duration.fromNanos(authenticateTimeout.toNanos))

  val bufferKilobytes = config.getBytes("buffer.kilobytes")
  println(bufferKilobytes)

  val bufferKiB = config.getBytes("buffer.kib")
  println(bufferKiB)

  val bufferKibibytes = config.getBytes("buffer.kibibytes")
  println(bufferKibibytes)
}