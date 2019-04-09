package com.proofpoint.scratch

import java.io.{Closeable, IOException}
import java.nio.file.{Files, Paths}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
