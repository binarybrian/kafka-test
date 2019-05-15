package com.proofpoint
package factory

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.zip.{Deflater, ZipEntry, ZipOutputStream}

import com.itextpdf.text.pdf.PdfWriter
import com.itextpdf.text.{BaseColor, Chunk, Document, FontFactory}
import com.proofpoint.factory.DocumentFactory.{charset, newlineBytes}
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.pdfbox.pdmodel.{PDDocument, PDPage, PDPageContentStream}

import scala.collection.JavaConverters._
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

  def toFile(isCsv: Boolean = false): Path = {
    val suffix = if (isCsv) ".csv" else ".txt"
    val tempPath = Files.createTempFile(s"$filename", suffix)
    val lines = if (isCsv) words.grouped(10).map(_.mkString(",")).toSeq else words
    Files.write(tempPath, lines.asJava)
    println(s"Finished ${tempPath.toString}")
    tempPath
  }

  def toPdf: Path = {
    val tempPath = Files.createTempFile(s"$filename", ".pdf")
    val document = new Document()
    PdfWriter.getInstance(document, new FileOutputStream(tempPath.toFile))

    document.open()
    val font = FontFactory.getFont(FontFactory.HELVETICA, 13, BaseColor.BLACK)
    words.foreach(word => document.add(new Chunk(word)))

    document.close()
    tempPath
  }

  def toPdfBox: Path = {
    val tempPath = Files.createTempFile(s"$filename", ".pdf")

    val document = new PDDocument()
    val page = new PDPage()
    document.addPage(page)

    val contentStream = new PDPageContentStream(document, page)
    contentStream.setFont(PDType1Font.HELVETICA, 16)

    contentStream.beginText()
    words.grouped(10).map(_.mkString(",")).foreach(line => {
      contentStream.showText(line)
      contentStream.newLine()
    })
    contentStream.endText()


    contentStream.close()

    document.save(tempPath.toString)
    document.close()

    tempPath
  }
}