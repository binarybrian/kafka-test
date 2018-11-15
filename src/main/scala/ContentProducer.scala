import java.util.UUID

import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global

object RandomProducerApp extends App {
  val config = ConfigFactory.load().resolve()
  val producer = new KafkaMessageProducer(config)
  val numMessages = 10000000
  messageStream.take(numMessages).par.zipWithIndex.foreach {
    case (message, i) =>
      if (i % 10000 == 0) println(s"Sending $i")
      producer.send("load-test", message)
  }

  def messageStream: Stream[String] = {
    Stream.continually(UUID.randomUUID().toString)
  }
}