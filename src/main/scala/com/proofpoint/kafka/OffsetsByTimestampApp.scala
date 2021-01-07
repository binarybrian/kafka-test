package com.proofpoint.kafka

import org.apache.kafka.clients.consumer.KafkaConsumer

import java.util.Properties

object OffsetsByTimestampApp extends App {
  val props = new Properties
  props.put("boostrap.servers", "kafka01.ius1.infoprtct.com:6667")
  props.put("group.id", "text-extractor-v2")
  val consumer = new KafkaConsumer(props)
}
