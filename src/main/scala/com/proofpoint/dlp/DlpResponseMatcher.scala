package com.proofpoint.dlp

import com.proofpoint.incidents.models.DlpResponse
import com.typesafe.config.Config

trait DlpResponseMatcher {
  private val consumer = new DlpResponseConsumer(config, this)

  def stop(): Unit = consumer.stop()

  def config: Config
  def matchResponse(dlpResponse: DlpResponse)

  consumer.start()
}
