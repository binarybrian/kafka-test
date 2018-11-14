import java.util.Properties
import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}

class KafkaEventConsumer(config: Config, topic: String, messageProcessor: MessageProcessor) {
  private val bootstrapServers = config.getString("kafka.bootstrap.servers")

  val properties: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-load-test")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p
  }

  val builder: StreamsBuilder = new StreamsBuilder
  builder.stream[Array[Byte], String](topic)
    .foreach((_, value) => messageProcessor.processMessage(value))

  val streams: KafkaStreams = new KafkaStreams(builder.build(), properties)
  streams.start()

  sys.ShutdownHookThread {
    streams.close(10, TimeUnit.SECONDS)
  }
}
