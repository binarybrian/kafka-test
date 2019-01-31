package com

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.ConnectException
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64.{getDecoder, getEncoder}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.google.common.io.ByteStreams
import com.proofpoint.commons.util.using

import scala.io.Source

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
            outputStream => ByteStreams.copy(decompressingInputStream, outputStream)
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
    }
  }

  def checkServiceStatusAll(): Unit = {
    Seq("jessica-jones" -> "http://localhost:9000", "watson" -> "http://localhost:9001", "sherlock" -> "http://localhost:9002").foreach({
      case (serviceName, url) => checkServiceStatus(serviceName, url)
    })
  }

  def curl(url: String): String = Source.fromURL(url).mkString
}
