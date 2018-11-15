package sim.json

import java.io.InputStream

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_ABSENT
import com.fasterxml.jackson.databind.DeserializationFeature.{FAIL_ON_NULL_CREATOR_PROPERTIES, FAIL_ON_UNKNOWN_PROPERTIES}
import com.fasterxml.jackson.databind.PropertyNamingStrategy.SNAKE_CASE
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper

object Json {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.configure(FAIL_ON_NULL_CREATOR_PROPERTIES, true)
  mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setSerializationInclusion(NON_ABSENT)
  mapper.setPropertyNamingStrategy(SNAKE_CASE)
  mapper.registerModule(DefaultScalaModule)

  def toString[A](a: A): String = {
    mapper.writeValueAsString(a)
  }

  def asBytes[A](a: A): Array[Byte] = {
    mapper.writeValueAsBytes(a)
  }

  def asJsonNode[A](a: A): JsonNode = {
    mapper.convertValue(a, classOf[JsonNode])
  }

  def parse[A: Manifest](content: String): A = {
    mapper.readValue(content)
  }

  def parse[A: Manifest](inputStream: InputStream): A = {
    mapper.readValue[A](inputStream)
  }

  def as[A: Manifest](any: Any): A = {
    Json.parse[A](toString(any))
  }
}
