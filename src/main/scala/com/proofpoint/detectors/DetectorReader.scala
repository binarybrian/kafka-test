package com.proofpoint.detectors

import java.nio.file.{Files, Paths}

import play.api.libs.json.Json

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.io.Source

case class RepoProfile(name: String, url: String, repoGroup: String)

object DetectorReader extends App {
  val dev = RepoProfile("dev", "https://policy.idev.infoprtct.com:9443", "repo_group_e27c3d550d024b729fd19cec5dc29d48")
  val stg = RepoProfile("stg", "https://policy.istg.infoprtct.com:9443", "repo_group_e27c3d550d024b729fd19cec5dc29d48")
  val us1 = RepoProfile("us1", "https://policy.ius1.infoprtct.com:9443", "repo_group_fec17d60646448e8b5c2f514820726f4")
  val eu2 = RepoProfile("eu2", "https://policy.ieu2.infoprtct.com:9443", "repo_group_01509f7ff06c444ca6dab7ca24a19a80")

  val isEu2 = true
  val eu2resource = if (isEu2) Source.fromResource("detectors-eu2.json") else Source.fromResource("detectors-us1.json")
  val env = if (isEu2) "EU2" else "US1"
  val tenantConfigs = Json.parse(eu2resource.getLines().mkString).as[Map[String, Seq[String]]]

  val totalSize = tenantConfigs.keySet.size
  println(s"[$env] There are $totalSize tenants TOTAL")

  val enabledAndDisabledAverage = tenantConfigs.values.map(_.size).sum / totalSize.toDouble
  println(s"[$env] The average number of detectors for ALL tenants is $enabledAndDisabledAverage")

  val tenantsWithEnabledDetectors = tenantConfigs.filter(_._2.nonEmpty)
  val enabledSize = tenantsWithEnabledDetectors.keySet.size
  println(s"[$env] There are $enabledSize tenants with ENABLED detectors")

  val enabledAverage = tenantsWithEnabledDetectors.values.map(_.size).sum / enabledSize.toDouble
  println(s"[$env] The average number of detectors for tenants with ENABLED detectors is ${enabledAverage}")
}

object UniqueRowColumns extends App {
  val path = Paths.get("/Users/bmerrill/workspace/weill/Cornell_EDM_with_test_data-copy2.csv")
  val result = Files.readAllLines(path).asScala.drop(1).zipWithIndex.map {
    case (line, index) =>
      val value = line.split(",", -1).toSeq.slice(3, 4).head
      (value, line)
  }.foldLeft(new mutable.HashMap[String, mutable.Set[String]] with mutable.MultiMap[String, String]) {
    (acc, pair) => acc.addBinding(pair._1, pair._2)
  }.filter(rows => {
    rows._2.size == 1
  }).map(_._2.head).take(30).mkString("\n")
  println(result)
}

object UniqueRowColumns2 extends App {
  val path = Paths.get("/Users/bmerrill/workspace/weill/Cornell_EDM_with_test_data-copy2.csv")

  val result = Files.readAllLines(path).asScala.reverse.take(100).zipWithIndex.map {
    case (line, index) =>
      val value = line.split(",", -1).toSeq.slice(3, 4).head
      (value, (index, line))
  }.foldLeft(new mutable.HashMap[String, mutable.Set[(Int, String)]] with mutable.MultiMap[String, (Int, String)]) {
    (acc, pair) => acc.addBinding(pair._1, pair._2)
  }.filter(rows => {
    rows._2.size == 1
  }).map(_._2.head).toSeq.sortWith((a, b) => a._1 < b._1).take(50).map(_._2.replace(',', '|')).mkString("\n")
  println(result)
}

object WordCount extends App {
  val path = Paths.get("/Users/bmerrill/workspace/weill/Cornell_EDM_with_test_data-copy2.csv")

  val allWords = Files.readAllLines(path).asScala.drop(1).foldLeft(new ArrayBuffer[String]()) {
    (acc, line) => acc ++= line.split(",", -1).filter(_.nonEmpty)
  }
  val allWordsSize = allWords.size
  val uniqueWordsSize = allWords.toSet.size
  println(s"All Values: $allWordsSize, Unique Values: $uniqueWordsSize, %Duplicate Values: ${((allWordsSize - uniqueWordsSize) / allWordsSize.toDouble) * 100}")
}

object ColumnCount extends App {
  val path = Paths.get("/Users/bmerrill/workspace/weill/Cornell_EDM_with_test_data-copy2.csv")

  val columnsByValue = Files.readAllLines(path).asScala.toList match {
    case head :: tail =>
      val columns = head.split(",", -1)
      val numColumns = columns.length
      tail.foldLeft(new mutable.HashMap[String, ListBuffer[String]]) {
        (acc, line) =>
          val columnValues = line.toLowerCase.split(",", -1)
          (0 until numColumns).foreach { index =>
            val columnName = columns(index)
            val allColumnValues = acc.getOrElseUpdate(columnName, new ListBuffer[String]())
            val columnValue = columnValues(index)
            if (columnValue.nonEmpty) allColumnValues.append(columnValue)
          }
          acc
      }
    case other => throw new Exception(s"Expected a list of size 2 or greater: ${other.getClass}")
  }
  val columnStats = columnsByValue.map {
    case (key, values) =>
      val allValues = values.length
      val uniqueValues = values.toSet.size
      //      if (key == "PAT_LAST_NAME") {
      //          val lastNameHistogram = values.foldLeft(new mutable.HashMap[String, Int]) {
      //            (acc, name) =>
      //              val currentValue = acc.getOrElseUpdate(name, 1)
      //              acc.put(name, currentValue + 1)
      //              acc
      //          }
      //        lastNameHistogram.foreach {
      //          case (key, value) => println(s"$key: $value")
      //        }
      //      }
      val percentUnique = (uniqueValues / allValues.toDouble) * 100d
      f"$key: $percentUnique%.1f" + "%"
  }
  val mrn = columnsByValue("MRN")
  val lastName = columnsByValue("PAT_LAST_NAME")
  val ssn = columnsByValue("SSN")
  val phone = columnsByValue("HOME_PHONE")

  val mrnLastName = (mrn.toSet.size + lastName.toSet.size) / (mrn.length + lastName.length).toDouble * 100d
  val mrnPhone = (mrn.toSet.size + phone.toSet.size) / (mrn.length + phone.length).toDouble * 100d
  val mrnSSN = (mrn.toSet.size + ssn.toSet.size) / (mrn.length + ssn.length).toDouble * 100d
  val lastNamePhone = (lastName.toSet.size + phone.toSet.size) / (lastName.length + phone.length).toDouble * 100d
  val lastNameSSN = (lastName.toSet.size + ssn.toSet.size) / (lastName.length + ssn.length).toDouble * 100d
  val phoneSSN = (phone.toSet.size + ssn.toSet.size) / (phone.length + ssn.length).toDouble * 100d

  println("Percent unique per column:")
  columnStats.foreach(println)
  println
  println(f"MRN && PAT_LAST_NAME: $mrnLastName%.1f" + "%")
  println(f"MRN && HOME_PHONE: $mrnPhone%.1f" + "%")
  println(f"MRN && SSN: $mrnSSN%.1f" + "%")
  println(f"PAT_LAST_NAME && HOME_PHONE: $lastNamePhone%.1f" + "%")
  println(f"PAT_LAST_NAME && SSN: $lastNameSSN%.1f" + "%")
  println(f"HOME_PHONE && SSN: $phoneSSN%.1f" + "%")
}