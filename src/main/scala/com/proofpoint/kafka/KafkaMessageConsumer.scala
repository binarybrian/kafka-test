package com.proofpoint.kafka

import java.util.Properties
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.typesafe.config.Config
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}

class KafkaMessageConsumer(config: Config, topic: String, messageProcessor: MessageProcessor) extends Logging {
  import Serdes._

  private val bootstrapServers = config.getString("kafka.bootstrap.servers")

  private val counter = new AtomicInteger(0)
  private val startTime = new AtomicLong(System.currentTimeMillis())

  private val properties: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-load-test")
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    p
  }

  private val builder: StreamsBuilder = new StreamsBuilder
  builder.stream[Array[Byte], String](topic)
    .foreach((key, value) => {
      val blah = new String(key)
      println(blah)
      messageProcessor.processMessage(value)
      val count = counter.getAndIncrement()
      val delta = System.currentTimeMillis() - startTime.get()
      if (delta >= 1000) {
        logger.info(s"Consumption rate: $count messages / second")
        startTime.set(System.currentTimeMillis())
        counter.set(0)
      }
    })

  private val streams: KafkaStreams = new KafkaStreams(builder.build(), properties)

  def start(): Unit = {
    logger.info(s"Starting $topic message consumer...")
    streams.start()
  }

  sys.ShutdownHookThread {
    streams.close(10, TimeUnit.SECONDS)
  }
}
