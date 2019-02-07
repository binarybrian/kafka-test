package com.proofpoint
package factory

import java.io.{BufferedOutputStream, OutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.zip.{Deflater, ZipEntry, ZipOutputStream}

import com.proofpoint.factory.DocumentFactory.{charset, newlineBytes}

import scala.collection.mutable.ListBuffer

abstract class DocumentFactory {
  def stream: Stream[String]

  def makeDocumentOfSizeBytes(filename: String, numBytes: Int): DlpDocument = {

    val acc = ListBuffer[String]()
    var byteCount = 0L

    val words = stream.takeWhile(word => {
      val byteSize = word.getBytes(charset).length
      val isAdded = (byteCount + byteSize + newlineBytes.length) < numBytes
      if (isAdded) {
        byteCount += (byteSize + newlineBytes.length)
        acc += word
        acc += System.lineSeparator()
      }
      isAdded
    })
    DlpDocument(filename, words)
  }
}

object DocumentFactory {
  val charset: String = "UTF-8"
  val newlineBytes: Array[Byte] = System.lineSeparator().getBytes(charset)
}

class WordOrNumberDocumentFactory() extends DocumentFactory {
  override def stream: Stream[String] = Words.wordOrNumberStream
}

case class DlpDocument(filename: String, words: Seq[String]) {
  def toZipFile: Path = {
    val zipFile = Files.createTempFile(s"$filename", ".zip")
    using(new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(zipFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)))) {
      zipStream =>
        zipStream.setLevel(Deflater.DEFAULT_COMPRESSION)
        zipStream.putNextEntry(new ZipEntry(filename))
        words.foreach(word => {
          zipStream.write(word.getBytes(charset))
          zipStream.write(newlineBytes)
        })
        zipStream.closeEntry()
    }
    zipFile
  }
}