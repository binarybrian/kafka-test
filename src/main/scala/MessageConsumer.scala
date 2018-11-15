trait MessageConsumer {
  def processMessage(message: String): Unit
}
