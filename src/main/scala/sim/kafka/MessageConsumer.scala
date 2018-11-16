package sim.kafka

trait MessageConsumer {
  def processMessage(message: String): Unit
}
