import java.util.{Properties, UUID}
import java.util.concurrent.TimeUnit

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.typesafe.config.{Config, ConfigFactory}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.streams.StreamsConfig
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.ExecutionContext

class KafkaEventProducer (config: Config)(implicit executionContext: ExecutionContext) extends Logging {
  private val bootstrapServers = config.getString("kafka.bootstrap.servers")

  private val properties = new Properties()
  properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  properties.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  private val producer = new KafkaProducer[String, String](properties)

  def send(topic: String, event: String): Unit = {
    producer.send(new ProducerRecord[String, String](topic, event), callback)
  }

  private def callback(metadata: RecordMetadata, exception: Exception): Unit = {
    if (exception != null) logger.error(s"Unable to send message: $metadata", exception)
  }

  sys.ShutdownHookThread {
    producer.close(10, TimeUnit.SECONDS)
  }
}


object ProducerApp extends App {
  val config = ConfigFactory.load().resolve()
  val producer = new KafkaEventProducer(config)
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

//Get number of messages in a topic -- https://stackoverflow.com/questions/28579948/java-how-to-get-number-of-messages-in-a-topic-in-apache-kafka/47313863#47313863
