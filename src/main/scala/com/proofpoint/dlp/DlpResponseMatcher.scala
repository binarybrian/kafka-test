package com.proofpoint.dlp

import com.proofpoint.incidents.models.DlpResponse

trait DlpResponseMatcher {
  def matchResponse(dlpResponse: DlpResponse)
}
