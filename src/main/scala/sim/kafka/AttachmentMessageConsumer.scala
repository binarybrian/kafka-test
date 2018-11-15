package sim.kafka

import sim.json.Json

class AttachmentMessageConsumer extends MessageConsumer {
  override def processMessage(message: String): Unit = {
    val attachment = Json.parse[Attachment](message)
  }
}
