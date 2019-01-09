package com.proofpoint.dlp

import java.net.ConnectException
import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
import java.util.UUID

import com.proofpoint.{checkServiceStatus, compress}
import com.proofpoint.dlp.DlpSuite._
import com.proofpoint.tika.TikaExtract
import com.typesafe.config.{Config, ConfigFactory}
import org.omg.CORBA.portable.ResponseHandler

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.io.Source
import scala.util.Random

case class DlpSuite(name: String, resourceFiles: Seq[String], numTests: Int, weights: Seq[Double] = Seq.empty) {
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
  val oneSmallSuite = DlpSuite("OneSmall", "dlp_small.docx", 1)
  val oneLargeSuite = DlpSuite("OneLarge", "dlp_large.docx", 1)
  val smallSuite = DlpSuite("small", "dlp_small.docx", 4000)
  val largeSuite = DlpSuite("large" ,"dlp_large.docx", 4)
  val mixedSuite = DlpSuite("mixed", Seq("dlp_small.docx", "dlp_large.docx"), 40, Seq(0.9, 0.1))

  def apply(name: String, resourceFile: String, numTests: Int) = new DlpSuite(name, Seq(resourceFile), numTests)
}

class DlpSuiteManager(config: Config) extends DlpResponseMatcher {
  private val producer = new DlpRequestProducer(config)
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
      request.logKey -> startTime
    }).toMap

    dlpRequests.foreach(producer.sendRequest)

    f
  }

  def matchResponse(message: DlpResponse): Unit = {
    val logKey = message.logKey
    if (!waitingDlp.contains(logKey)) {
      println(s"Skipping unknown logKey $logKey")
    }
    else {
      finishedDlpBuilder += (logKey -> (System.currentTimeMillis() - waitingDlp(logKey)))
      val finishedDlp = finishedDlpBuilder.result()
      if (finishedDlp.size == waitingDlp.size) {
        p.success(finishedDlp)
      }
    }
  }

  def shutdown(): Unit = {
    producer.close()
    consumer.stop()
  }
}

object DlpSuiteManager {
  def openResource(resourceName: String): String = {
    val tempDir = Paths.get(System.getProperty("java.io.tmpdir"))
    val filePath = tempDir.resolve(resourceName)
    if (Files.exists(filePath)) {
      //println(s"Reading from temporary resource $filePath")
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
}

object DlpTestSuiteApp extends App {
  def runSuite(tenantId: String, dlpSuite: DlpSuite): Map[String, Long] = {
    val contents = (1 to dlpSuite.numTests).map(_ => {
      val nextResource = dlpSuite.nextResource
      DlpSuiteManager.openResource(nextResource)
    }).toVector

    println(s"Running suite ${dlpSuite.name} with ${contents.length} messages")
    val requestStart = System.currentTimeMillis()
    val responseTimes = Await.result(manager.sendContents(tenantId, contents), Duration.Inf)
    println(s"Finished suite ${dlpSuite.name} dlp total response time ${System.currentTimeMillis() - requestStart} ms")

    responseTimes
  }

  checkServiceStatus("Sherlock", "http://localhost:9000")

  val config = ConfigFactory.load()
  val manager = new DlpSuiteManager(config)

  val tenantId = "tenant_a9998b6b7083490784afda48dd383928"
  val suite = oneSmallSuite

  val responseTimes = runSuite(tenantId, suite)
  val avg = responseTimes.values.sum / responseTimes.values.size

  println(s"Suite ${suite.name} dlp avg response time $avg ms for ${responseTimes.values.size} messages")

  manager.shutdown()
}
