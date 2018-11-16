package sim.kafka

import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.typesafe.config.{Config, ConfigFactory}
import sim.json.Json
import sim.kafka.FileProducer.randomStringStream
import sim.s3.S3

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}
import scala.util.{Failure, Success}

case class Attachment(bucket: String, link: String)

class FileProducer(config: Config) extends Logging {
  private val s3 = new S3
  private val bucket = config.getString("s3.bucket")
  private val topic = config.getString("kafka.topic.s3file")

  private val producer = new KafkaMessageProducer(config)

  def sendFiles(promise: Promise[Unit]): Unit = {
    val counter = new AtomicInteger(0)
    val numMessages = 100
    val logStep = Math.max(numMessages / 100, 10)
    randomStringStream.take(numMessages).zipWithIndex.foreach {
      case (filename, i) =>
        s3.upload(i.toString, bucket, filename).map(_ => Json.toString(Attachment(bucket, s"https://$bucket.s3.amazonaws.com/$filename"))).onComplete {
          case Success(attachmentJson) =>
            producer.send(topic, attachmentJson)
            val count = counter.incrementAndGet()
            if (count % logStep == 0) logger.info(s"Sent $i / $numMessages message")
            if (count == numMessages) promise.success(())
          case Failure(exception) => exception.printStackTrace()
        }
    }
  }
}

object FileProducer {
  def randomStringStream: Stream[String] = {
    Stream.continually(UUID.randomUUID().toString)
  }
}

object RandomProducerApp extends App {
  val config = ConfigFactory.load().resolve()
  val producer = new KafkaMessageProducer(config)
  val numMessages = 10000000
  randomStringStream.take(numMessages).par.zipWithIndex.foreach {
    case (message, i) =>
      if (i % 10000 == 0) println(s"Sending $i")
      producer.send("load-test", message)
  }
}

object FileProducerApp extends App {
  println("Starting file producer...")
  val config = ConfigFactory.load().resolve()
  val promise = Promise[Unit]
  val fileProducer = new FileProducer(config)
  fileProducer.sendFiles(promise)
  Await.result(promise.future, Duration.Inf)
  println("Finished file producer.")
}