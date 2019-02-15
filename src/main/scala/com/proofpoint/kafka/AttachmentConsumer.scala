package com.proofpoint.kafka

import com.proofpoint.commons.json.Json._
import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.s3.S3
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

class AttachmentConsumer(config: Config) extends KafkaMessageConsumer(config, config.getString("kafka.topic.attachment")) with Logging {
  private val s3 = new S3

  override def processMessage(message: String): Unit = {
    val attachment = message.as[Attachment]
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
  val consumer = new AttachmentConsumer(config)
  consumer.start()
}


