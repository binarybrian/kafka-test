package sim.kafka

import java.util.Properties
import java.util.concurrent.TimeUnit

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.typesafe.config.Config
import org.apache.kafka.clients.producer.ProducerConfig._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord, RecordMetadata}
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsConfig

import scala.concurrent.ExecutionContext

class KafkaMessageProducer(config: Config)(implicit executionContext: ExecutionContext) extends Logging {
  private val bootstrapServers = config.getString("kafka.bootstrap.servers")

  private val properties = new Properties()
  properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
  properties.put(KEY_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)
  properties.put(VALUE_SERIALIZER_CLASS_CONFIG, classOf[StringSerializer].getName)

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

