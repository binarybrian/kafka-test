package com.proofpoint.dlp

import com.fasterxml.jackson.databind.JsonNode

case class DlpResponse(ruleEngine: String, logKey: String, tenantId: String, result: ContentAnalysisResult, rawData: Option[JsonNode] = None, sourceMetadata: Option[JsonNode] = None)
case class ContentAnalysisResult(detectorMatches: Seq[DetectorMatch])
case class DetectorMatch(id: String, name: String, segments: Seq[Snippet])
case class Snippet(snippetOffset: Int, content: String, offset: Int, length: Int)