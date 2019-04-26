name := "kafka-test"

version := "0.1"

scalaVersion := "2.12.8"

resolvers += "Artifactory" at "https://firelayers.jfrog.io/firelayers/internal-scala-artifactory/"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "com.itextpdf" % "itextpdf" % "5.5.13",
  "com.proofpoint" %% "commons-logging" % "1.0.4",
  "com.proofpoint" %% "commons-tokenizer" % "1.0.6",
  "com.proofpoint" %% "incident-models" % "2.7.0-DPR-2155-slack-im-attach.104",
  "com.typesafe" % "config" % "1.3.3",
  "log4j" % "log4j" % "1.2.17" % Runtime,
  "net.codingwell" %% "scala-guice" % "4.2.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "5.2" % Runtime,
  "org.apache.kafka" %% "kafka-streams-scala" % "2.1.0",
  "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "6.7.1",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0",
  "software.amazon.awssdk" % "s3" % "2.2.0",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.475",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.475",
  "com.github.seratch" %% "awscala" % "0.8.1",
  "org.apache.pdfbox" % "pdfbox" % "2.0.13",
  "org.apache.tika" % "tika-core" % "1.20",
  "org.apache.tika" % "tika-parsers" % "1.20"
)
