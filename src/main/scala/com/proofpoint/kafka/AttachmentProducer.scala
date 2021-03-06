package com.proofpoint.kafka

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.commons.json.Implicits._
import com.proofpoint.kafka.AttachmentProducer.randomStringStream
import com.proofpoint.s3.S3
import com.typesafe.config.{Config, ConfigFactory}
import play.api.libs.json.{Format, Json}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}

case class Attachment(bucket: String, filename: String, link: String)

object Attachment {
  implicit val format: Format[Attachment] = Json.format[Attachment]
}

class AttachmentProducer(config: Config) extends KafkaMessageProducer(config) with Logging {
  private val s3 = new S3
  private val bucket = config.getString("s3.bucket")

  override val topic: String = config.getString("kafka.topic.attachment")

  def sendFiles(promise: Promise[Unit]): Unit = {
    val counter = new AtomicInteger(0)
    val numMessages = 100
    val logStep = Math.max(numMessages / 100, 10)
    randomStringStream.take(numMessages).zipWithIndex.foreach {
      case (filename, i) =>
        s3.upload(i.toString, bucket, filename).map(_ => Attachment(bucket, filename, s"https://$bucket.s3.amazonaws.com/$filename").stringify).onComplete {
          case Success(attachmentJson) =>
            sendMessage(topic, attachmentJson)
            val count = counter.incrementAndGet()
            if (count % logStep == 0) logger.info(s"Sent $i / $numMessages message")
            if (count == numMessages) promise.success(())
          case Failure(exception) => exception.printStackTrace()
        }
    }
  }
}

object AttachmentProducer {
  def randomStringStream: Stream[String] = {
    Stream.continually(UUID.randomUUID().toString)
  }
}

object AttachmentProducerApp extends App {
  val config = ConfigFactory.load().resolve()
  val promise = Promise[Unit]
  val fileProducer = new AttachmentProducer(config)

  println("Starting attachment producer...")
  fileProducer.sendFiles(promise)
  Await.result(promise.future, Duration.Inf)
  println("Finished attachment producer.")
}

object RandomProducerApp extends App {
  val config = ConfigFactory.load().resolve()
  val producer = new KafkaMessageProducer(config) {
    override val topic: String = "load-test"
  }

  val numMessages = 10000000
  randomStringStream.take(numMessages).par.zipWithIndex.foreach {
    case (message, i) =>
      if (i % 10000 == 0) println(s"Sending $i")
      producer.sendMessage(message)
  }
}
