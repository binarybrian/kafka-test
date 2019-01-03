package com

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Base64.{getDecoder, getEncoder}
import java.util.zip.{GZIPInputStream, GZIPOutputStream}

import com.google.common.io.ByteStreams
import com.proofpoint.commons.util.using

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
}
