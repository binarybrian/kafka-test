package com.proofpoint.scratch

import java.io.{Closeable, IOException}
import java.nio.file.{Files, Paths}
import java.time.{Duration, Instant, LocalDateTime}
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import com.google.common.base.{CharMatcher, Splitter}

import scala.collection.JavaConverters._
import scala.collection.immutable.TreeSet
import scala.io.Source

object TimeFormatApp extends App {
  val fromPath = "/Users/bmerrill/workspace/temp/elb-es-02.csv"
  val toPath = "/Users/bmerrill/workspace/temp/elb-es-02-out.csv"
  closeable(Source.fromFile(fromPath)) {
    reader => {
      val formattedLines = reader.getLines().map(line => {
        val dateTimeUrl = Splitter.on(',').trimResults(CharMatcher.is(0xFEFF.toChar)).split(line).asScala.toVector
        val dateTime = dateTimeUrl(0)
        val url = dateTimeUrl(1)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'")
        val fromDateTime = LocalDateTime.parse(dateTime, formatter)
        //val toDateTime = fromDateTime.atZone(ZoneId.of("UTC")).toInstant.toEpochMilli
        //val fromDateTime = LocalDateTime.from(parsedDateTime)
        val toDateTime = fromDateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        s"$toDateTime,$url\n"
      })
      closeable(Files.newBufferedWriter(Paths.get(toPath))) {
        writer => {
          formattedLines.foreach(line => {
            writer.write(line)
          })
        }
      }
    }
  }

  println("Finished TimeFormatApp")

  def closeable[A <: Closeable, B](resource: A)(f: A => B): B = {
    try {
      f(resource)
    }
    finally {
      try {
        resource.close()
      }
      catch {
        case e: IOException => println("Exception closing resource", e)
      }
    }
  }
}

object TestTreeOrderApp extends App {
  val treeSet = TreeSet(("z", 3), ("y", 2), ("x", 1))(Ordering.by(_._2))
  treeSet.foreach(println)

  val blah = TreeSet("c", "b", "a")(Ordering[String])
  blah.foreach(println)

  val mutableSet = scala.collection.mutable.Set.empty ++= blah
  val immutableSet = Set.empty ++ mutableSet
  val immutableSet2 = Set(mutableSet.toSeq: _*)
  println(immutableSet2)
}

object TestTimer {
  def timer[F](name: String, f: => F): F = {
    val startTime = System.currentTimeMillis()
    val result = f
    println(s"Test took ${(System.currentTimeMillis() - startTime).toString}")
    result
  }
}

object HappyFunTime extends App {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration.Duration
  import scala.concurrent.{Await, Future}
  import scala.util.Random

  private def happyFuture(id: Int): Future[String] = {
    Future {
      if (id %5 == 0) throw new java.lang.Exception("Stinky Sad Reality")
      Thread.sleep(Random.nextInt(500))
      s"Finished $id"
    }
  }

  val parts = 1 until 10
  val futureHappys: Seq[Future[Option[String]]] = parts.map(happyFuture)
    .map(result => result.map(Some(_)).recover {
      case t => None
    })

  val results: Future[Seq[String]] = Future.sequence(futureHappys).map(_.flatten)
  println(Await.result(results, Duration.Inf))
}

object Timestamp extends App {
  //EU2 1568844638.363
  //US1 1568844209.039

  val startTime = Instant.ofEpochMilli(1568844209).minus(2, ChronoUnit.HOURS)
  val diff = Duration.between(startTime, Instant.now())
  println(s"$diff --> ${diff.toMillis}")
}

object TimeFormatKafka extends App {

}