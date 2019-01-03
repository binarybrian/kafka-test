package com.proofpoint.kafka

import java.time.Duration
import java.util.Properties

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.typesafe.config.Config
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

class KafkaMessageConsumer(config: Config, topic: String, messageProcessor: MessageProcessor) extends Logging {
  import Serdes._

  private val bootstrapServers = config.getString("kafka.bootstrap.servers")

  private val properties: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-load-test")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p
  }

  private val builder: StreamsBuilder = new StreamsBuilder
  builder.stream[Array[Byte], String](topic)
    .foreach((_, value) => messageProcessor.processMessage(value))

  private val streams: KafkaStreams = new KafkaStreams(builder.build(), properties)

  def start(): Unit = {
    logger.info(s"Starting $topic message consumer...")
    streams.start()
  }

  def stop(): Unit = streams.close(Duration.ofSeconds(60))

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(60))
  }
}
