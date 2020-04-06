package com.proofpoint.dlp

import com.proofpoint.incidents.models.DlpResponse
import com.typesafe.config.Config

trait DlpResponseMatcher {
  def matchResponse(dlpResponse: DlpResponse)
}
