package com.proofpoint.tika

import java.io.{File, InputStream}

import awscala.CredentialsLoader
import awscala.Region0.US_EAST_1
import awscala.s3.S3
import com.amazonaws.ClientConfiguration
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.{AutoDetectParser, ParseContext}
import org.apache.tika.sax.BodyContentHandler

object TikaExtract {
  def extract(inputStream: InputStream): String = {
    val tikaConfig = new TikaConfig(new File("conf/tika_conf.xml"))
    val parser = new AutoDetectParser(tikaConfig)
    val metadata = new Metadata()
    val handler = new BodyContentHandler(-1)
    val parseContext = new ParseContext

    parser.parse(inputStream, handler, metadata, parseContext)
    handler.toString
  }
}

object TikaExtractApp extends App{
  val s3Config = new ClientConfiguration()
  //val credentialsProvider = DefaultCredentialsProvider().getCredentials()
  val provider = CredentialsLoader.load
  val credentials = provider.getCredentials()
  val region = US_EAST_1
  val s3 = S3(s3Config, credentials)(region)
  val s3Object = s3.getObject("flp-dlp-dev", "a02f43764d05f3b48e54c9f7f18b7c4a")
  //val s3Object = s3.getObject("infoprtct-watson-dev", "tika_test.txt")
  val inputStream = s3Object.getObjectContent
  //val content = IOUtils.toString(inputStream).toLowerCase
  //println(s"Content Size: ${content.length} --> $content")

  val parser = new AutoDetectParser()
  val metadata = new Metadata()
  val handler = new BodyContentHandler()
  val parseContext = new ParseContext

  parser.parse(inputStream, handler, metadata, parseContext)

  val content = handler.toString
  inputStream.close()

  println(s"content: $content")
//  var value = 1
//  while (value > 0) {
//    value = inputStream.read()
//    println(value)
//  }
//
//  val tika = new Tika
//  val content = tika.parseToString(inputStream)
  //

  //AmazonS3ClientBuilder.standard().withCredentials(new)
//  val s3 = AmazonS3ClientBuilder.defaultClient()
//  try {
//    s3.putObject("flp-dlp-dev")
//  }
}
