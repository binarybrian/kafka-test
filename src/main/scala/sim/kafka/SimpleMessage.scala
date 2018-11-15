package sim.kafka

import java.nio.file.Files
import java.util.UUID

import com.typesafe.config.ConfigFactory
import sim.json.Json
import sim.kafka.SimpleMessage.randomStringStream
import sim.s3.S3

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success}

case class Attachment(bucket: String, link: String)

object SimpleMessage {
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
  val s3 = new S3
  val config = ConfigFactory.load().resolve()
  val producer = new KafkaMessageProducer(config)
  val bucket = config.getString("s3.bucket")
  val topic = config.getString("kafka.topic.s3file")

  val numMessages = 100
  randomStringStream.take(numMessages).zipWithIndex.foreach {
    case (filename, i) =>
      s3.upload(i.toString, bucket, filename).onComplete {
        case Success(()) =>
          if (i % Math.max(numMessages / 100, 10) == 0) println(s"Sent $i / $numMessages message")
          producer.send(topic, Json.toString(Attachment(bucket, s"https://$bucket.s3.amazonaws.com/$filename")))
        case Failure(exception) => exception.printStackTrace()
      }
  }
}