package com.proofpoint.dlp

import com.fasterxml.jackson.databind.JsonNode

case class DlpRequest(logKey: String, tenantId: String, content: String, rawData: Option[JsonNode] = None, sourceMetadata: Option[JsonNode] = None)
