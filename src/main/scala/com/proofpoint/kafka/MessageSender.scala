package com.proofpoint.kafka

trait MessageSender {
  def sendMessage(topoic: String, message: String): Unit
}
