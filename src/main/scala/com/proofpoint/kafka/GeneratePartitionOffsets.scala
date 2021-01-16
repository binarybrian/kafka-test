package com.proofpoint.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.consumer.{KafkaConsumer, OffsetAndTimestamp}
import org.apache.kafka.common.TopicPartition

import java.nio.file.{Files, Paths}
import java.time.{ZoneId, ZonedDateTime}
import java.util.Properties
import scala.collection.JavaConverters._

/*
 * 1. List offsets and make a backup of current offsets
 * kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka01.ius1.infoprtct.com:6667 --group text-extractor-v2 --describe | tee text-extractor-v2-offsets.txt
 * 2. Find partition(s) to reset from step 1
 * 3. Determine reset time
 * 4. Set time in GenerateOffsetResetsApp
 * 5. Set partitions to reset in GenerateOffsetResetsApp
 * 6. Generate reset.csv
 * 7. Disable consumers.  Deploy with replicas and max-replicas set to 0
 * 8. Execute reset
 * kafka/bin/kafka-consumer-groups.sh --bootstrap-server kafka01.ius1.infoprtct.com:6667 --topic text_extraction_request_v2 --group text-extractor-v2 --reset-offsets --from-file reset.csv --execute
 * 9. Enable consumers.  Deploy with replicas set to non-zero value
*/

object GeneratePartitionOffsets extends App {
  val broker = "kafka01.ius1.infoprtct.com:6667"
  val groupId = "text-extractor-v2" //sherlock group is "sherlock"
  val topic = "text_extraction_request_v2" //sherlock topic is "dlp_request_fast" or "edm_response_fast"
  val partitions = Set(51)

  val year = 2021; val month = 1; val dayOfMonth = 15; val hour = 0; val minute = 20; val seconds = 0; val nanoSeconds = 0
  val zonedTime: ZonedDateTime = ZonedDateTime.of(year, month, dayOfMonth, hour, minute, seconds, nanoSeconds, ZoneId.systemDefault())
  val timestamp: Long = zonedTime.toInstant.toEpochMilli

  val resetFilename = "reset.csv"

  val props = new Properties
  props.put("bootstrap.servers", broker)
  props.put("group.id", groupId)
  props.put("key.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
  props.put("value.deserializer","org.apache.kafka.common.serialization.StringDeserializer")
  val resetOffsets = offsetsAtTimestamp(topic, partitions, timestamp).map {
    case (key, value) => key.partition() -> value.offset()
  }

  val adminClient = AdminClient.create(props)
  val offsets = adminClient.listConsumerGroupOffsets(groupId).partitionsToOffsetAndMetadata().get().asScala
  val rows = offsets.map {
    case (topicAndPartition, offsetAndMetadata) =>
      val resetPartition = topicAndPartition.partition
      resetOffsets.get(resetPartition).foreach { replacedOffset =>
        println(s"Replacing offset ${offsetAndMetadata.offset()} with $replacedOffset for partition $resetPartition")
      }
      val replacedOffset = resetOffsets.getOrElse(resetPartition, offsetAndMetadata.offset)
      List(topicAndPartition.topic, topicAndPartition.partition, replacedOffset).mkString(",")
  }

  val kafkaPath = "/testpool/pfpt/ka/kafka"
  val basePath = System.getProperty("user.dir")
  val writePath = Paths.get(basePath, resetFilename)
  Files.write(writePath, rows.asJava)
  println(s"Finished writing new offsets to $writePath.  Test for errors by running the following command.  Change '--dry-run' to '--execute' when ready:")
  val command = s"$kafkaPath/bin/kafka-consumer-groups.sh --bootstrap-server $broker --topic $topic --group $groupId --reset-offsets --from-file $basePath/$resetFilename --dry-run"
  println(command)

  def offsetsAtTimestamp(topic: String, partitions: Set[Int], timestamp: Long): Map[TopicPartition, OffsetAndTimestamp] = {
    val consumer = new KafkaConsumer(props)
    val timestampsToSearch = partitions.map { partition =>
      new TopicPartition(topic, partition) -> timestamp
    }.toMap.mapValues(long2Long).view.force.asJava
    consumer.offsetsForTimes(timestampsToSearch).asScala.toMap
  }
}