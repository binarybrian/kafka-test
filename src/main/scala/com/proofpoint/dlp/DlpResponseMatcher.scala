package com.proofpoint.dlp

trait DlpResponseMatcher {
  def matchResponse(dlpResponse: DlpResponse)
}
