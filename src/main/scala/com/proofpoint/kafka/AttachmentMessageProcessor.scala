package com.proofpoint.kafka

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.json.Json
import com.proofpoint.s3.S3
import com.typesafe.config.ConfigFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

class AttachmentMessageProcessor extends MessageProcessor with Logging {
  private val s3 = new S3

  override def processMessage(message: String): Unit = {
    val attachment = Json.parse[Attachment](message)
    s3.downloadToString(attachment.bucket, attachment.filename)
      .andThen {
        case Success(_) => s3.deleteObject(attachment.bucket, attachment.filename)
        case Failure(exception) => logger.error(s"Failed to download attachment ${attachment.filename}", exception)
      }
      .onComplete {
        case Success(content) if content.toInt % 15 == 0 => logger.info("FizzBuzz")
        case Success(content) if content.toInt % 5 == 0 => logger.info("Buzz")
        case Success(content) if content.toInt % 3 == 0 => logger.info("Fizz")
        case Failure(exception) => logger.error(s"Failed to process ${attachment.filename}", exception)
      }
  }
}

object AttachmentConsumerApp extends App {
  val config = ConfigFactory.load().resolve()
  val attachmentTopic = config.getString("kafka.topic.attachment")

  val messageProcessor = new AttachmentMessageProcessor
  val consumer = new KafkaMessageConsumer(config, attachmentTopic, messageProcessor)
  consumer.start()
}

object DeleteBucketApp extends App {

  import scala.concurrent.ExecutionContext.Implicits.global

  val bucketName = "infoprtct-watson-dev"
  val s3 = new S3
  val s3Objects = Await.result(s3.objects(bucketName), Duration.Inf)
  s3Objects.foreach(s3Object => {
    println(s"Deleting ${s3Object.key()}")
    Await.result(s3.deleteObject(bucketName, s3Object.key()), Duration.Inf)
  })
}
