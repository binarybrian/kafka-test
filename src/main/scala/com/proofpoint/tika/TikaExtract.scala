package com.proofpoint.tika

import java.io.{File, InputStream}
import java.nio.file.{Files, Paths}
import java.util.concurrent.atomic.AtomicBoolean

import awscala.CredentialsLoader
import awscala.Region0.US_EAST_1
import awscala.s3.S3
import com.amazonaws.ClientConfiguration
import com.proofpoint.tika.TikaExtract.maxExtractLength
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.{BodyContentHandler, ContentHandlerDecorator}

object TikaExtract {
  val maxExtractLength = 20000000

  def extract(inputStream: InputStream): String = {
    val tikaConfig = new TikaConfig(new File("conf/tika_conf.xml"))
    val parser = new AutoDetectParser(tikaConfig)
    val metadata = new Metadata()
    val handler = new SizeLimitContentHandler()
    //val handler = new BodyContentHandler(-1)
    val parseContext = new ParseContext

    parser.parse(inputStream, handler, metadata, parseContext)
    if  (handler.exceededLength.get()) {
      println("Tika extraction truncated.")
    }
    handler.content.toString()
  }
}

class SizeLimitContentHandler extends ContentHandlerDecorator {
  val content = new StringBuilder
  val exceededLength = new AtomicBoolean(false)

  override def characters(ch: Array[Char], start: Int, length: Int): Unit = {
    if (!exceededLength.get()) {
      if (length < maxExtractLength && content.length < maxExtractLength) {
        val appendLength = if (content.length + length > maxExtractLength) {
          exceededLength.set(true)
          maxExtractLength - content.length
        }
        else {
          length
        }
        content.appendAll(ch, start, appendLength)
      }
      else {
        exceededLength.set(true)
      }

      if (exceededLength.get()) {
        println(s"Tika extracted length ${content.length + length} exceeds max length $maxExtractLength. Partially extracted content length ${content.length}")
      }
    }
  }

  override def ignorableWhitespace(ch: Array[Char], start: Int, length: Int): Unit = {
    characters(ch, start, length)
  }
}

object S3TikaExtractApp extends App {
  val s3Config = new ClientConfiguration()
  val provider = CredentialsLoader.load
  val credentials = provider.getCredentials()
  val region = US_EAST_1
  val s3 = S3(s3Config, credentials)(region)
  val s3Object = s3.getObject("flp-dlp-dev", "a02f43764d05f3b48e54c9f7f18b7c4a")
  val inputStream = s3Object.getObjectContent

  val parser = new AutoDetectParser()
  val metadata = new Metadata()
  val handler = new BodyContentHandler(-1)
  val parseContext = new ParseContext

  parser.parse(inputStream, handler, metadata, parseContext)

  val content = handler.toString
  inputStream.close()

  println(s"content: $content")

}

object LocalTikaExtractApp extends App {
  val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/md-test.md"))

  val startTime = System.currentTimeMillis()
  val content = TikaExtract.extract(inputStream)
  val endTime = System.currentTimeMillis() - startTime

  println(content)

  println(s"Extracted ${content.length} bytes in $endTime ms")
}