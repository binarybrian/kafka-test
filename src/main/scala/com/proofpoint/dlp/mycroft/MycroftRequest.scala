package com.proofpoint
package dlp.mycroft

import java.nio.file.{Files, Path, Paths}

import com.decodified.scalassh.{HostConfig, HostKeyVerifiers, PublicKeyLogin, SSH, SshClient, SshLogin}
import com.proofpoint.commons.efs.EfsService
import com.proofpoint.commons.efs.EfsService.{BasePathProperty}
import com.proofpoint.commons.json.Implicits._
import com.proofpoint.commons.logging.SimpleLoggingContext
import com.proofpoint.commons.metrics.fake.FakeMetricService
import com.proofpoint.commons.util.RawTranscoder
import com.proofpoint.commons.util.compression.CompressionAlgorithm.GZIP
import com.proofpoint.commons.util.compression.GZipCompressor
import com.proofpoint.incidents.models.DlpRequest
import com.proofpoint.kafka.AttachmentProducerApp.promise
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import net.schmizz.sshj.common.IOUtils
import net.schmizz.sshj.transport.verification.{ConsoleKnownHostsVerifier, PromiscuousVerifier}
import play.api.libs.json.{Format, JsObject, Json}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.Try

case class MycroftRequest(requestId: String, context: RequestContext, requestDetails: JsObject, sendResponseTo: String, contentParts: Map[String, MycroftContentPart]) {
  lazy val loggingContext: SimpleLoggingContext = new SimpleLoggingContext(s"$requestId ${context.customerId}")
}

object MycroftRequest {
  implicit val format: Format[MycroftRequest] = Json.format[MycroftRequest]
}

//object MakeMycroftKafkaRequest extends App {
//  val content = Map("text" -> MycroftContentPart("ssn 122-33-4444"))
//
//  val context = RequestContext("8fc6e791-64ad-49f9-8a11-d8d5903db83c")
//  val request = MycroftRequest("request-12345", context, JsObject.empty, "localhost:9000", content)
//  println(request.stringify)
//
//
//  val contentMap = request.contentParts.mapValues(_.toContentPart).view.force.filter(_._2.content.nonEmpty)
//  val mycroftContent = contentMap.stringify
//  val encodedContent = RawTranscoder.encode(mycroftContent, Some(GZipCompressor))
//  val dlpRequest = DlpRequest(request.loggingContext.toString, request.context.customerId, None, Some("fd63023b-c473-45ea-9926-8dd5f6e84893/temp.pfpt"), JsObject.empty)
//  println(dlpRequest.stringify)
//}

//object MakeMycroftEfsRequest extends App {
//  import com.proofpoint.commons.logging.Implicits.NoLoggingContext
//
//  val buffer = Source.fromFile("/Users/bmerrill/workspace/pfpt/ScanCloud/big_files/efwefwe.txt")
//  val content = Map("text" -> MycroftContentPart(buffer.mkString))
//
//  val context = RequestContext("8fc6e791-64ad-49f9-8a11-d8d5903db83c")
//  val request = MycroftRequest("request-12345", context, JsObject.empty, "localhost:9000", content)
//
//  private val directory = Files.createTempDirectory("")
//  println(s"Temp directory $directory")
//
//  System.setProperty("PRIVATELYSHARED_DATATRANSFER_DECRYPTIONKEY", "1:AbYGigkcjPYafHgtTnlBh7Q=")
//  System.setProperty("PRIVATELYSHARED_DATATRANSFER_ENCRYPTIONKEY", "1:AbYGigkcjPYafHgtTnlBh7Q=")
//
//  private val config: Config = ConfigFactory.empty
//    .withValue(BasePathProperty, ConfigValueFactory.fromAnyRef(directory.toAbsolutePath.toString))
//    .withValue("PRIVATELYSHARED_DATATRANSFER_DECRYPTIONKEY", ConfigValueFactory.fromAnyRef("1:AbYGigkcjPYafHgtTnlBh7Q="))
//    .withValue("PRIVATELYSHARED_DATATRANSFER_ENCRYPTIONKEY", ConfigValueFactory.fromAnyRef("1:AbYGigkcjPYafHgtTnlBh7Q="))
//    .withValue(MetricPrefixProperty, ConfigValueFactory.fromAnyRef("MycroftTest"))
//  val efsService = new EfsService(config, FakeMetricService)
//
//
//  val contentMap = request.contentParts.mapValues(_.toContentPart).view.force.filter(_._2.content.nonEmpty)
//  val mycroftContent = contentMap.stringify
//  //val encodedContent = RawTranscoder.encode(mycroftContent, Some(GZipCompressor))
//  val efsPath = Paths.get(directory.toString, efsService.write(mycroftContent, GZIP, Some("datatransfer")).toString)
//  println(s"EFSPath: $efsPath")
//
//
//  //val dlpRequest = DlpRequest(request.loggingContext.toString, request.context.customerId, Some(encodedContent), None, JsObject.empty)
//  //println(dlpRequest.stringify)
//}

object SendMycroftRequest extends App {

}

object UmDoesThisWork extends App {
  println("Running Ummmm....")

  val promise = Promise[Unit]

  val sshLogin = PublicKeyLogin("bmerrill")
  val myHostConfig = HostConfig(sshLogin, hostKeyVerifier = HostKeyVerifiers.DontVerify)
  SSH("10.51.5.210", myHostConfig) { client: SshClient =>
    println(s"We have a connection. Are we authenticated? ${client.client.isAuthenticated}")
    client.authenticatedClient
    println(s"We have a connection. Are we authenticated? ${client.client.isAuthenticated}")
    for {
      result <- {
        println("Executing command")
        client.exec("ls -la")
      }
    } println(s"!!!!! $result")
  }

  Await.result(promise.future, Duration.Inf)
}

object Boogers extends App {

  import net.schmizz.sshj.SSHClient
  import java.util.concurrent.TimeUnit

  import java.io.Console

  private val con = System.console

  val ssh: SSHClient = new SSHClient
  ssh.loadKnownHosts()
  ssh.addHostKeyVerifier("fa:7e:36:94:79:fe:a3:c5:af:78:0f:97:db:6e:a1:13")
  ssh.connect("10.51.5.210")
  ssh.authPublickey(System.getProperty("user.name"))
  val session = ssh.startSession()
  println(ssh.isConnected)
  println(ssh.isAuthenticated)
  val command = session.exec("ls -la")
  val is = command.getInputStream
  val commandResponse = if (is != null) Source.fromInputStream(is).mkString
  val errorStream = command.getErrorStream
  val errorResponse = if (errorStream != null) Source.fromInputStream(errorStream).mkString

  command.join(5, TimeUnit.SECONDS)

  println(s"??? $commandResponse")
}