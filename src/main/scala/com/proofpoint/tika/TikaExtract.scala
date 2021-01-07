package com.proofpoint.tika

import java.io.{File, InputStream, OutputStreamWriter}
import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.Instant
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import awscala.CredentialsLoader
import awscala.Region0.US_EAST_1
import awscala.s3.S3
import com.amazonaws.ClientConfiguration
import com.proofpoint.commons.logging.LoggingUtils.formatMillis
import com.proofpoint.commons.logging.{Logging, LoggingContext}
import com.proofpoint.tika.TikaExtract.maxExtractLength
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import org.apache.tika.Tika
import org.apache.tika.config.{TikaConfig, TikaConfigSerializer}
import org.apache.tika.exception.EncryptedDocumentException
import org.apache.tika.extractor.EmbeddedDocumentExtractor
import org.apache.tika.metadata.Metadata
import org.apache.tika.parser.pdf.PDFParser
import org.apache.tika.parser.{AutoDetectParser, CompositeParser, ParseContext}
import org.apache.tika.sax.{BodyContentHandler, ContentHandlerDecorator}
import org.xml.sax.{ContentHandler, SAXException}

import scala.collection.JavaConverters._
import scala.util.Try

object TikaExtract {
  val maxExtractLength = 20000000

  def extract(inputStream: InputStream): String = {
    val tikaConfig = new TikaConfig(new File("conf/tika_conf.xml"))
    val parser = new AutoDetectParser(tikaConfig)
    val metadata = new Metadata()
    val handler = new SizeLimitContentHandler()
    //val handler = new BodyContentHandler(-1)

    val embeddedPath = new AtomicReference[Path]()
    val embeddedDocumentExtractor = new EmbeddedDocumentExtractor() {
      override def shouldParseEmbedded(metadata: Metadata): Boolean = {
        val names = metadata.names().toVector
        names.foreach(name => {
          println(s"$name --> ${metadata.get(name)}")
        })
        true
      }

      override def parseEmbedded(stream: InputStream, handler: ContentHandler, metadata: Metadata, outputHtml: Boolean): Unit = {
        println("I AM HERE 22222 !!!!")
        val tempFile = Files.createTempFile(Instant.now().toEpochMilli.toString, "image")
        try {
          println(s"Copying embedded image to ${tempFile.toString} -- available: ${stream.available()}")
          Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }
        catch {
          case e: Exception => println(s"Failed to write to $tempFile with ${e.getMessage} -- ${e.printStackTrace()}")
        }
        embeddedPath.set(tempFile)
      }
    }
    val context = new ParseContext
    context.set(classOf[EmbeddedDocumentExtractor], embeddedDocumentExtractor)

    parser.parse(inputStream, handler, metadata, context)
    if (handler.exceededLength.get()) {
      println("Tika extraction truncated.")
    }
    println(s"!!!! Metadata: $metadata with $embeddedPath")

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
  //val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/md-test.md"))
  //val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/pfpt/hackday/embedded-image-dlp.pdf"))
  //val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/kellogg1.docx"))
  val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/foobaz.temp"))

  val startTime = System.currentTimeMillis()
  val content = TikaExtract.extract(inputStream)
  val endTime = System.currentTimeMillis() - startTime

  println(content)
}

object SimpleTikaExtractApp extends App {
  //val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/Tenants/weill/allfp/AAAA-Notification of DLP Email Incident abm2013@med.cornell.edu (1).eml"))
  val inputStream = Files.newInputStream(Paths.get("/Users/bmerrill/workspace/Tenants/weill/research-200401/Frontline COVID-19 Experience - March 2020.pdf"))

  val parser = new AutoDetectParser()
  val metadata = new Metadata()
  val handler = new BodyContentHandler(-1)
  parser.parse(inputStream, handler, metadata)
  val content = handler.toString

  println(content)
}

object TikaBenchmarkApp extends App {
  import com.proofpoint.commons.logging.Implicits.NoLoggingContext

  val sourceDir = "/Users/bmerrill/dev/tika/tika-parsers/src/test/resources/test-documents/"
  val config = ConfigFactory.empty().withValue("content.max-size-bytes", ConfigValueFactory.fromAnyRef("16 MiB"))
  val statsFile = Files.createFile(Paths.get(System.getProperty("user.dir")).resolve(s"tika-bench-${System.currentTimeMillis()}.txt"))

  val bench = new TikaBenchmark(config)
  val files = bench.getFiles(sourceDir)

  val header = Seq("time,length,file")
  val tikaVersion = new Tika(TikaConfig.getDefaultConfig).toString

  println(s"Starting benchmark with $tikaVersion...")
  val results = files.map { file =>
    val path = file.toPath
    val startTime = System.currentTimeMillis()
    val content = bench.extract(Files.newInputStream(path))
    val elapsedTime = System.currentTimeMillis() - startTime
    (elapsedTime, content.length, path.getFileName)
  }

  val totalTime = results.map(_._1).sum
  val average = Try(totalTime / results.length).getOrElse(0L)
  val sortedTimes = results.filter(_._2 > 0).sortBy(_._1).map(_._1)
  val median = if (sortedTimes.isEmpty) 0 else {
    if (sortedTimes.length % 2 == 0) {
      val (left, right) = sortedTimes.splitAt(sortedTimes.length / 2)
      (left.last + right.head) / 2
    } else {
      sortedTimes(sortedTimes.length / 2)
    }
  }

  formatMillis(123)
  println(s"average: ${formatMillis(average)}, median: ${formatMillis(median)}, total: ${formatMillis(totalTime)}, files: ${files.length}, results: ${results.length}")

  val csvResults = header ++ results.map {
    case (time, length, file) => s"$time,$length,$file"
  }
  Files.write(statsFile, csvResults.asJava)
  println(s"Results at ${statsFile.toAbsolutePath}")
}

class TikaBenchmark(config: Config) extends Logging {
  val maxExtractSize: Int = config.getBytes("content.max-size-bytes").toInt
  val parser = new AutoDetectParser()

  def extract(inputStream: InputStream)(implicit loggingContext: LoggingContext): String = {
    val contentHandler = new BodyContentHandler(maxExtractSize)
    Try(parser.parse(inputStream, contentHandler, new Metadata)).recover {
      case _: SAXException => logger.warn("Extracted content exceeds max extract size")
      case _: EncryptedDocumentException => logger.warn("Skipped encrypted document")
      case e => logger.error(s"Tika extract failed with message ${e.getMessage}")
    }
    contentHandler.toString
  }

  def getFiles(dir: String): Seq[File] = {
    val dirFile = new File(dir)
    if (dirFile.exists() && dirFile.isDirectory) {
      dirFile.listFiles.toSeq
    }
    else {
      Seq.empty
    }
  }
}

object TikaTestApp extends App {
  val tikaConfig: TikaConfig = TikaConfig.getDefaultConfig
  val tika = new Tika(tikaConfig)
  println(tika.toString)
  TikaConfigSerializer.serialize(tikaConfig, TikaConfigSerializer.Mode.CURRENT, new OutputStreamWriter(System.out, UTF_8), UTF_8)
}

object CustomTikaTestApp extends App {
  val tikaConfig = new TikaConfig(getClass.getResourceAsStream("/tika-conf.xml"))
  val tika = new Tika(tikaConfig)
  TikaConfigSerializer.serialize(tikaConfig, TikaConfigSerializer.Mode.CURRENT, new OutputStreamWriter(System.out, UTF_8), UTF_8)

  tika.getParser match {
    case autoDetectParser: AutoDetectParser =>
      println(autoDetectParser.getAllComponentParsers.size())
      println(autoDetectParser.getAllComponentParsers.asScala.map(_.getClass.getCanonicalName).mkString(","))
      autoDetectParser.getAllComponentParsers.asScala.foreach {
        case compositeParser: CompositeParser =>
          compositeParser.getAllComponentParsers.asScala.foreach {
            case pdfParser: PDFParser =>
              val pdfConfig = pdfParser.getPDFParserConfig
              println(s"OCR Strategy: ${pdfConfig.getOcrStrategy}")
              println(s"MaxMainMemoryBytes: ${pdfConfig.getMaxMainMemoryBytes}")
              println(s"extract actions: ${pdfConfig.getExtractActions}")
              println(s"inline images: ${pdfConfig.getExtractInlineImages}")
              println(s"marked content: ${pdfConfig.getExtractMarkedContent}")
            case _ => ()
          }
      }
  }
}