name := "kafka-test"

version := "0.1"

scalaVersion := "2.12.7"

resolvers += "Artifactory" at "https://firelayers.jfrog.io/firelayers/internal-scala-artifactory/"
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

libraryDependencies ++= Seq(
  "com.proofpoint" %% "commons-logging" % "1.0.4",
  "com.typesafe" % "config" % "1.3.2",
  "net.codingwell" %% "scala-guice" % "4.2.1",
  "org.apache.kafka" %% "kafka-streams-scala" % "2.0.0"
)
