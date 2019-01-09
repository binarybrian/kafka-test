package com.proofpoint.dlp

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.json.Json
import com.proofpoint.kafka.{KafkaMessageConsumer, MessageProcessor}
import com.typesafe.config.Config

import scala.util.control.NonFatal

class DlpResponseConsumer(config: Config, dlpManager: DlpResponseMatcher) extends MessageProcessor with Logging {
  private val topic = config.getString("kafka.topic.dlp_response")
  private val consumer = new KafkaMessageConsumer(config, topic, this)

  override def processMessage(message: String): Unit = {
    try {
      dlpManager.matchResponse(Json.parse[DlpResponse](message))
    }
    catch {
      case NonFatal(e) =>
        val message = "Kafka message could not be processed"
        logger.warn(message, e)
        s"$message: $e"
    }
  }

  def start(): Unit = consumer.start()
  def stop(): Unit = consumer.stop()
}
