package com.proofpoint

import java.nio.file.{Files, Paths}
import java.time.Instant

import com.proofpoint.commons.json.Implicits._
import com.proofpoint.commons.logging.LoggingContext
import com.proofpoint.commons.util.RawTranscoder
import com.proofpoint.commons.util.compression.GZipCompressor
import com.proofpoint.dlp.api.{DictionaryConfiguration, DictionaryEntry, DictionaryEntryType, Metadata}
import com.proofpoint.dlp.impl.dictionary.DictionaryValidator.parse
import play.api.libs.json.{Format, Json}
import play.api.libs.json.Json.toJson

import collection.JavaConverters._

case class CompressedDictionary(metadata: Metadata, compressedEntries: String) {
  def entries: List[String] = Json.parse(RawTranscoder.decode(compressedEntries, Some(GZipCompressor))).as[List[String]]
  def toDictionary(implicit loggingContext: LoggingContext): DictionaryConfiguration = DictionaryConfiguration(metadata, parse(entries))
}

object CompressedDictionary {
  def apply(dictionary: DictionaryConfiguration): CompressedDictionary = CompressedDictionary(dictionary.metadata, RawTranscoder.encode(toJson(dictionary.entries.map(_.toEntry)).toString(), Some(GZipCompressor)))

  implicit val format: Format[CompressedDictionary] = Json.format[CompressedDictionary]
}

object DictionaryApp extends App {
  val entries = Files.readAllLines(Paths.get("/Users/bmerrill/workspace/pfpt/kellog/Kellogg/1.txt")).asScala.map(DictionaryEntry(1, None, DictionaryEntryType.CaseInsensitive, _)).toVector
  val time = Instant.now().toEpochMilli
  val dictionaryConfiguration = DictionaryConfiguration(Metadata("kellog", "kellog", None, None, time, time), entries)
  val compressedDictionary = CompressedDictionary(dictionaryConfiguration)
  println(compressedDictionary.stringify)
}
