name := "kafka-test"

version := "0.1"

scalaVersion := "2.12.11"

resolvers += "Artifactory" at "https://firelayers.jfrog.io/firelayers/internal-scala-artifactory/"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1",
  "com.decodified" %% "scala-ssh" % "0.10.0",
  "com.itextpdf" % "itextpdf" % "5.5.13.1",
  "com.proofpoint" %% "commons-efs" % "0.30.0",
  "com.proofpoint" %% "commons-util" % "0.30.0",
  "com.proofpoint" %% "commons-logging" % "1.2.0",
  "com.proofpoint" %% "commons-tokenizer" % "1.1.0",
  "com.proofpoint" %% "incident-models" % "2.28.0",
  "com.proofpoint" %% "unified-dlp" % "0.15.0",
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-http" % "10.1.11",
  "log4j" % "log4j" % "1.2.17" % Runtime,
  "net.codingwell" %% "scala-guice" % "4.2.2",
  "net.logstash.logback" % "logstash-logback-encoder" % "5.2" % Runtime,
  "org.apache.kafka" % "kafka-clients" % "2.1.0",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.1.0",
  "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % "6.7.1",
  "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.1",
  "software.amazon.awssdk" % "s3" % "2.11.9",
  "com.amazonaws" % "aws-java-sdk-core" % "1.11.759",
  "com.amazonaws" % "aws-java-sdk-s3" % "1.11.759",
  "com.github.seratch" %% "awscala" % "0.8.1",
  "com.github.blemale" %% "scaffeine" % "3.1.0",
  "com.github.ben-manes.caffeine" % "caffeine" % "2.8.0",
  "org.apache.pdfbox" % "pdfbox" % "2.0.19",
  "org.apache.tika" % "tika-core" % "1.24",
  "org.apache.tika" % "tika-parsers" % "1.24"
)
