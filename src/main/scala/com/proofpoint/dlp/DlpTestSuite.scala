package com.proofpoint.dlp

import java.net.ConnectException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.proofpoint.compress
import com.proofpoint.tika.TikaExtract
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.io.Source
import scala.util.Random

case class DlpSuite(resourceFiles: Seq[String], numTests: Int, weights: Seq[Double] = Seq.empty) {
  def nextResource: String = {
    if (weights.isEmpty || weights.length != resourceFiles.length) {
      resourceFiles(Random.nextInt(resourceFiles.size))
    }
    else {
      resourceFiles(weightIndex(weights))
    }
  }

  def weightIndex(weights: Seq[Double]): Int = {
    val totalWeight = weights.sum
    var randomWeight = Math.random * totalWeight
    weights.indices.foreach {
      i =>
        randomWeight -= weights(i)
        if (randomWeight <= 0d) {
          return i
        }
    }
    throw new Exception("Could not find weight index.")
  }

}

object DlpSuite {
  val simpleSuite = DlpSuite("dlp_small.docx", 1)
  val largeSuite = DlpSuite("dlp_large.docx", 10)
  val mixedSuite = DlpSuite(Seq("dlp_small.docx", "dlp_large.docx"), 100, Seq(0.8, 0.2))
  val smallSuite = DlpSuite("dlp_small.docx", 1000)

  def apply(resourceFile: String, numTests: Int) = new DlpSuite(Seq(resourceFile), numTests)
}

class DlpTestSuite(config: Config) {
  private val producer = new DlpRequestProducer(config, this)
  private val consumer = new DlpResponseConsumer(config, this)

  private val p = Promise[Map[String, Long]]()
  private val f: Future[Map[String, Long]] = p.future

  private var waitingDlp = Map.empty[String, Long]
  private val finishedDlpBuilder = Map.newBuilder[String, Long]

  consumer.start()

  def sendContents(tenantId: String, contents: Seq[String]): Future[Map[String, Long]] = {
    val dlpRequests = contents.zipWithIndex.map {
      case (content, index) => DlpRequest(s"$index-${UUID.randomUUID().toString}", tenantId, compress(content))
    }

    waitingDlp = dlpRequests.map(request => {
      val startTime = System.currentTimeMillis()
      producer.send(request)
      request.logKey -> startTime
    }).toMap

    f
  }

  def matchResponse(message: DlpResponse): Unit = {
    val logKey = message.logKey
    finishedDlpBuilder += (logKey -> (System.currentTimeMillis() - waitingDlp(logKey)))
    val finishedDlp = finishedDlpBuilder.result()
    if (finishedDlp.size == waitingDlp.size) {
      p.success(finishedDlp)
    }
  }

  def shutdown(): Unit = {
    producer.close()
    consumer.stop()
  }
}

object DlpTestSuite {
  def openResource(resourceName: String): String = {
    val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
    val filePath = tempDir.resolve(resourceName)
    if (Files.exists(filePath)) {
      println(s"Reading from temporary resource $filePath")
      Files.readAllLines(filePath).asScala.mkString("\n")
    }
    else {
      println(s"Temporary file not found.  Extracting content to $filePath")
      val content = extract(resourceName)
      Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
      content
    }
  }

  def extract(resourceName: String): String = {
    val startTime = System.currentTimeMillis()
    val inputStream = getClass.getResourceAsStream(s"/$resourceName")
    val content = TikaExtract.extract(inputStream)
    println(s"Extract time: ${System.currentTimeMillis() - startTime} ms")
    content
  }

  def curl(url: String): String = Source.fromURL(url).mkString
}

object DlpTestSuiteApp extends App {
  def runSuite(tenantId: String, dlpSuite: DlpSuite): Map[String, Long] = {
    val contents = (1 to dlpSuite.numTests).map(_ => {
      val nextResource = dlpSuite.nextResource
      DlpTestSuite.openResource(nextResource)
    }).toVector

    println("Checking Sherlock status...")
    try {
      val sherlockResponse = DlpTestSuite.curl("http://localhost:9000")
      if (!sherlockResponse.toLowerCase.contains("sherlock")) {
        println("Sherlock is not running. Shutting down...")
        System.exit(1)
      }
    }
    catch {
      case e: ConnectException =>
        println("Sherlock is not running. Shutting down...")
        System.exit(1)
    }

    println("Sending dlp requests...")
    val requestStart = System.currentTimeMillis()
    val responseTimes = Await.result(manager.sendContents(tenantId, contents), Duration.Inf)
    println(s"dlp total response time ${System.currentTimeMillis() - requestStart} ms")

    responseTimes
  }

  val config = ConfigFactory.load()
  val manager = new DlpTestSuite(config)

  val tenantId = "tenant_a9998b6b7083490784afda48dd383928"
  val suite = DlpSuite.mixedSuite

  val responseTimes = runSuite(tenantId, suite)
  val avg = responseTimes.values.sum / responseTimes.values.size
  println(s"dlp avg response time $avg ms for ${responseTimes.values.size} messages")

  manager.shutdown()
}
