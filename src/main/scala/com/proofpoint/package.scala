package com

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, Closeable, IOException}
import java.net.ConnectException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64.{getDecoder, getEncoder}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.google.common.io.ByteStreams
import play.api.libs.json.{Json, JsonConfiguration}
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase

import scala.io.Source
import scala.util.control.NonFatal

package object proofpoint {
  def timer[F](name: String, f: => F): (F, Long) = {
    val startTime = System.currentTimeMillis()
    val result = f
    val delta = System.currentTimeMillis() - startTime
    result -> delta
  }

  def compress(sourceString: String): String = {
    if (sourceString.length == 0) ""
    else {
      val outputStream = new ByteArrayOutputStream(sourceString.length)
      using(new GZIPOutputStream(getEncoder.wrap(outputStream))) {
        compressingOutputStream => {
          using(new ByteArrayInputStream(sourceString.getBytes(UTF_8))) {
            inputStream => ByteStreams.copy(inputStream, compressingOutputStream)
          }
        }
      }

      new String(outputStream.toByteArray, UTF_8)
    }
  }

  def decompress(encodedString: String): String = {
    if (encodedString.length == 0) ""
    else {
      using(new GZIPInputStream(getDecoder.wrap(new ByteArrayInputStream(encodedString.getBytes(UTF_8))))) {
        decompressingInputStream => {
          using(new ByteArrayOutputStream(encodedString.length)) {
            outputStream =>
              ByteStreams.copy(decompressingInputStream, outputStream)
              new String(outputStream.toByteArray, UTF_8)
          }
        }
      }
    }
  }

  def checkServiceStatus(serviceName: String, url: String): Unit = {
    println(s"Checking $serviceName status...")
    try {
      val response = curl(url)
      if (!response.toLowerCase.contains(serviceName)) {
        println(s"$serviceName is not running. Shutting down...")
        System.exit(1)
      }
    }
    catch {
      case e: ConnectException =>
        println(s"$serviceName is not running. Shutting down...")
        System.exit(1)
      case i: IOException =>
        println(s"$serviceName returned ${i.getMessage}.  Shutting down...")
        System.exit(1)
    }
  }

  def checkServiceStatusAll(): Unit = {
    Seq(
      "jessica-jones" -> "http://localhost:9000",
      "watson" -> "http://localhost:9001",//Sherlock hard codes Watson to localhost:9001 for testing.
      "sherlock" -> "http://localhost:9002",
      "mycroft" -> "http://localhost:9003")
      .foreach({
        case (serviceName, url) => checkServiceStatus(serviceName, url)
      })
  }

  def curl(url: String): String = Source.fromURL(url).mkString

  def using[A <: Closeable, B](resource: A)(f: A => B): B = {
    var exception: Throwable = null
    try {
      f(resource)
    }
    catch {
      case NonFatal(e) =>
        exception = e
        throw e
    }
    finally {
      if (exception != null) {
        try {
          resource.close()
        }
        catch {
          case NonFatal(e) => exception.addSuppressed(e)
        }
      }
      else {
        resource.close()
      }
    }
  }

  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)
}
