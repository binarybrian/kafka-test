package com.proofpoint.kafka

trait MessageProcessor {
  def processMessage(message: String): Unit
}
