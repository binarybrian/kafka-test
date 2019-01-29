package com.proofpoint.dlp

import com.proofpoint.commons.logging.Implicits.NoLoggingContext
import com.proofpoint.commons.logging.Logging
import com.proofpoint.incidents.models.DlpResponse
import com.proofpoint.json.Json
import com.proofpoint.kafka.KafkaMessageConsumer
import com.typesafe.config.Config

import scala.util.control.NonFatal

class DlpResponseConsumer(config: Config, dlpResponseMatcher: DlpResponseMatcher) extends KafkaMessageConsumer(config, config.getString("kafka.topic.dlp_response")) with Logging {
  override def processMessage(message: String): Unit = {
    try {
      dlpResponseMatcher.matchResponse(Json.parse[DlpResponse](message))
    }
    catch {
      case NonFatal(e) =>
        val message = "Kafka message could not be processed"
        logger.warn(message, e)
        s"$message: $e"
    }
  }
}
