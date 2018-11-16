package com.proofpoint.kafka

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.json.Json
import com.proofpoint.s3.S3
import com.typesafe.config.ConfigFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class AttachmentProcessor extends MessageProcessor with Logging {
  private val s3 = new S3

  override def processMessage(message: String): Unit = {
    val attachment = Json.parse[Attachment](message)
    s3.downloadToString(attachment.bucket, attachment.filename)
      .andThen {
        case Success(_) => s3.deleteObject(attachment.bucket, attachment.filename)
        case Failure(exception) => logger.error(s"Failed to download attachment ${attachment.filename}", exception)
      }
      .onComplete {
        case Success(content) => logger.info(s"Consumed $content")
        case Failure(exception) => logger.error(s"Failed to process ${attachment.filename}", exception)
    }
  }
}

object AttachmentConsumerApp extends App {
  val config = ConfigFactory.load().resolve()
  val attachmentTopic = config.getString("kafka.topic.attachment")

  val messageProcessor = new AttachmentProcessor
  val consumer = new KafkaMessageConsumer(config, attachmentTopic, messageProcessor)
  consumer.start()
}
