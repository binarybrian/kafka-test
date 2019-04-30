package com.proofpoint
package es

import java.io.Closeable
import java.util.UUID.randomUUID

import com.proofpoint.commons.json.Implicits._
import com.proofpoint.incidents.models.{ApplicationType, ChannelSource, ChannelType, Incident, IncidentStatus, SaaSFileSourceMetadata, SummaryInfo}
import org.apache.http.HttpHost
import org.elasticsearch.action.DocWriteRequest.OpType
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.index.{IndexRequest, IndexResponse}
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.indices.{CreateIndexRequest, GetIndexRequest, GetMappingsRequest}
import org.elasticsearch.client.{RequestOptions, RestClient, RestHighLevelClient}
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import play.api.libs.json.JsObject

import scala.collection.JavaConverters._

class EasyBake(val indexName: String, hostname: String = "localhost", port: Int = 9200, scheme: String = "http") extends Closeable {
  val client = new RestHighLevelClient(RestClient.builder(new HttpHost(hostname, port, scheme)))

  def createIndex(indexName: String = this.indexName): Boolean = {
    val getIndexRequest = new GetIndexRequest(indexName)
    val indexExists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT)
    if (!indexExists) {
      val createIndexRequest = new CreateIndexRequest(indexName)
      createIndexRequest.settings(EasyBake.dlpSettings, XContentType.JSON)
      createIndexRequest.mapping(EasyBake.dlpMappings, XContentType.JSON)
      client.indices().create(createIndexRequest, RequestOptions.DEFAULT).isAcknowledged
    }
    else true
  }

  def updateSettings(indexName: String = this.indexName): Boolean = {
    val updateSettingsRequest = new UpdateSettingsRequest(indexName)
    updateSettingsRequest.settings(EasyBake.dlpSettings, XContentType.JSON)
    val response = client.indices().putSettings(updateSettingsRequest, RequestOptions.DEFAULT)
    response.isAcknowledged
  }

  def deleteIndex(indexName: String = this.indexName): Boolean = {
    val deleteRequest = new DeleteIndexRequest(indexName)
    client.indices().delete(deleteRequest, RequestOptions.DEFAULT).isAcknowledged
  }

  def writeDocument(id: String, document: String, indexName: String = this.indexName): Int = {
    val indexRequest = new IndexRequest(indexName)
    indexRequest.id(id)
    indexRequest.`type`("_doc")
    indexRequest.source(document, XContentType.JSON)
    val response = client.index(indexRequest, RequestOptions.DEFAULT)
    response.status().getStatus
  }

  def writeDocuments(documents: Seq[String], indexName: String = this.indexName): Unit = {
    val bulkRequest = new BulkRequest()
    documents.zipWithIndex.foreach {
      case (document, documentIndex) =>
        val id = documentIndex.toString
        val indexRequest = new IndexRequest(indexName)
        indexRequest.id(id)
        indexRequest.`type`("_doc")
        indexRequest.source(document, XContentType.JSON)
        bulkRequest.add(indexRequest)
    }
    val response = client.bulk(bulkRequest, RequestOptions.DEFAULT)
    response.asScala.foreach(itemResponse => itemResponse.getOpType match {
      case OpType.INDEX | OpType.CREATE =>
        val indexResponse = itemResponse.getResponse.asInstanceOf[IndexResponse]
        println(s"CreateResponse: ${indexResponse.toString}")
      case OpType.UPDATE =>
        val updateResponse = itemResponse.getResponse.asInstanceOf[UpdateResponse]
        println(s"UpdateResponse: ${updateResponse.toString}")
      case OpType.DELETE =>
        val deleteResponse = itemResponse.getResponse.asInstanceOf[DeleteResponse]
        println(s"DeleteResponse: ${deleteResponse.toString}")
    })
  }

  def getDocuments(indexName: String = this.indexName): String = {
    val searchRequest = new SearchRequest(indexName)
    val searchSourceBuilder = new SearchSourceBuilder()
    searchSourceBuilder.query(QueryBuilders.matchAllQuery())
    searchRequest.source(searchSourceBuilder)
    val response = client.search(searchRequest, RequestOptions.DEFAULT)
    response.getHits.getHits.toVector.map(hit => hit.toString).mkString("{\n", ",\n", "\n}")
  }

  def totalDocuments(indexName: String = this.indexName): Long = {
    val searchRequest = new SearchRequest(indexName)
    val searchSourceBuilder = new SearchSourceBuilder()
    searchSourceBuilder.query(QueryBuilders.matchAllQuery())
    searchRequest.source(searchSourceBuilder)
    val response = client.search(searchRequest, RequestOptions.DEFAULT)
    response.getHits.totalHits
  }

  def getMappings(indexName: String = this.indexName): String = {
    val getMappingsRequest = new GetMappingsRequest()
    getMappingsRequest.indices(indexName)
    val response = client.indices().getMapping(getMappingsRequest, RequestOptions.DEFAULT)
    response.mappings().asScala.get(indexName) match {
      case Some(indexMapping) =>
        indexMapping.source().toString
      case None =>
        s"No mapping found for $indexName"
    }
  }

  def getIndices: String = {
    val getIndexRequest = new GetIndexRequest("*")
    val response = client.indices().get(getIndexRequest, RequestOptions.DEFAULT)
    response.getIndices.mkString(",")
  }

  override def close(): Unit = {
    client.close()
  }
}

object EasyBake {
  val indexName = "blah"
  val dlpMappings = """{"properties":{"id":{"type":"keyword"},"tenant_id":{"type":"keyword"},"channel_type":{"type":"keyword"},"metadata_type":{"type":"keyword"},"channel_source":{"type":"keyword"},"application_type":{"type":"keyword"},"entity_name":{"type":"keyword","fields":{"partial_match":{"type":"text","analyzer":"partial_match_analyzer"},"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"}}},"flagged":{"type":"boolean"},"status":{"type":"keyword"},"summary_info":{"properties":{"detector_matches":{"properties":{"detector_match_name":{"type":"keyword","fields":{"partial_match":{"type":"text","analyzer":"partial_match_analyzer"}}},"match_count":{"type":"integer"}}}}},"time":{"type":"date"},"last_updated_time":{"type":"date"},"users":{"type":"nested","properties":{"id":{"type":"keyword"},"display_name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"}}},"name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"word_partial_match_analyzer"}}},"email":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"partial_match_analyzer"}}},"service_provider_id":{"type":"keyword"},"groups":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword"}}},"ip":{"type":"keyword"},"location":{"properties":{"country":{"type":"keyword"},"country_iso_code":{"type":"keyword"},"city":{"type":"keyword"},"region":{"type":"keyword"}}}}},"source_metadata":{"properties":{"id":{"type":"keyword"},"channel_source":{"type":"keyword"},"channel_type":{"type":"keyword"},"metadata_type":{"type":"keyword"},"origin":{"type":"keyword"},"share_level":{"type":"integer"},"created_timestamp":{"type":"date"},"modified_timestamp":{"type":"date"},"size":{"type":"long"},"title":{"type":"keyword"},"extension":{"type":"keyword"},"mimetype":{"type":"keyword"},"location":{"type":"keyword"},"link":{"type":"keyword"},"trashed":{"type":"boolean"},"subject":{"type":"keyword"},"room_type":{"type":"keyword"},"frame_start_timestamp":{"type":"date"},"frame_end_timestamp":{"type":"date"},"owner":{"properties":{"id":{"type":"keyword"},"name":{"type":"keyword"},"email":{"type":"keyword"}}},"sent_timestamp":{"type":"date"},"sender":{"properties":{"id":{"type":"keyword"},"name":{"type":"keyword"},"email":{"type":"keyword"}}},"from":{"properties":{"name":{"type":"keyword"},"email":{"type":"keyword"}}},"to_recipients":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"word_partial_match_analyzer"}}},"email":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"partial_match_analyzer"}}},"display_name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"}}},"service_provider_id":{"type":"keyword"},"groups":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword"}}},"ip":{"type":"keyword"},"location":{"properties":{"country":{"type":"keyword"},"country_iso_code":{"type":"keyword"},"city":{"type":"keyword"},"region":{"type":"keyword"}}}}},"cc_recipients":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"word_partial_match_analyzer"}}},"email":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"partial_match_analyzer"}}},"display_name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"}}},"service_provider_id":{"type":"keyword"},"groups":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword"}}},"ip":{"type":"keyword"},"location":{"properties":{"country":{"type":"keyword"},"country_iso_code":{"type":"keyword"},"city":{"type":"keyword"},"region":{"type":"keyword"}}}}},"bcc_recipients":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"word_partial_match_analyzer"}}},"email":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"},"partial_match":{"type":"text","analyzer":"partial_match_analyzer"}}},"display_name":{"type":"keyword","fields":{"case_insensitive":{"type":"keyword","normalizer":"case_insensitive_normalizer"}}},"service_provider_id":{"type":"keyword"},"groups":{"type":"nested","properties":{"id":{"type":"keyword"},"name":{"type":"keyword"}}},"ip":{"type":"keyword"},"location":{"properties":{"country":{"type":"keyword"},"country_iso_code":{"type":"keyword"},"city":{"type":"keyword"},"region":{"type":"keyword"}}}}}}},"content_analysis_result":{"enabled":false},"matches":{"type":"nested","properties":{"detector_id":{"type":"keyword"},"detector_name":{"type":"keyword"},"snippets":{"type":"nested","properties":{"context_type":{"type":"keyword"}}}}}}}"""
  val dlpSettings ="""{"analysis":{"analyzer":{"partial_match_analyzer":{"type":"custom","tokenizer":"partial_match_tokenizer","filter":["lowercase"]},"word_partial_match_analyzer":{"type":"custom","tokenizer":"word_partial_match_tokenizer","filter":["lowercase"]}},"tokenizer":{"partial_match_tokenizer":{"type":"nGram","min_gram":"1","max_gram":"3","token_chars":["letter","digit","punctuation","symbol","whitespace"]},"word_partial_match_tokenizer":{"type":"nGram","min_gram":"1","max_gram":"3","token_chars":["letter","digit"]}},"normalizer":{"case_insensitive_normalizer":{"type":"custom","filter":["lowercase"]}}}}"""
}

object CreateIndex extends App {
  val result = using(new EasyBake(EasyBake.indexName)) {
    client => {
      client.createIndex()
    }
  }
  println(s"Created index '${EasyBake.indexName}' with result $result")
}

object DeleteIndex extends App {
  using(new EasyBake(EasyBake.indexName)) {
    client => {
      val response = client.deleteIndex()
      println(s"Deleted index '${client.indexName}' with response $response")
    }
  }
}

object PopulateIndex extends App {
  val numIncidents = 1000
  val easyBake = new EasyBake(EasyBake.indexName)
  val response = easyBake.createIndex()
  println(s"Creating $numIncidents documents...")
  val incidents = (1 to numIncidents).map(_ => IncidentGenerator.createLegacyIncident).toVector
  easyBake.writeDocuments(incidents)
  easyBake.close()
//  (1 to numIncidents).map(index => {
//    val incident = IncidentGenerator.createLegacyIncident
//    easyBake.writeDocument(index.toString, incident)
//  })
}

object PrintIndexMappings extends App {
  using(new EasyBake(EasyBake.indexName)) {
    client => {
      println(client.getMappings())
    }
  }
}

object PrintIndices extends App {
  using(new EasyBake(EasyBake.indexName)) {
    client => {
      println(client.getIndices)
    }
  }
}

object PrintDocuments extends App {
  using(new EasyBake(EasyBake.indexName)) {
    easyBake => println(s"~~~~ ${easyBake.getDocuments().take(10000)} ~~~~")
  }
}

object PrintTotalDocuments extends App {
  using(new EasyBake(EasyBake.indexName)) {
    easyBake => println(s"~~~~ ${easyBake.totalDocuments()} ~~~~")
  }
}

object IncidentGenerator {
  def createLegacyIncident: String = {
    val id = randomUUID().toString.replace("-", "")
    val tenantId = "tenant_12345"
    val sourceMetadata = SaaSFileSourceMetadata(ChannelType.SaaSFile, id, tenantId, ChannelSource.PCASB, ApplicationType.Slack, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None, None)
    val incident = Incident(id, tenantId, ChannelType.SaaSFile, ChannelSource.PCASB, ApplicationType.Slack, flagged = false, None, SummaryInfo(Seq.empty), IncidentStatus.New, System.currentTimeMillis(), System.currentTimeMillis(), Seq.empty, sourceMetadata, None, None)
    val incidentJson = incident.as[JsObject]
    val legacyJson = incidentJson - "metadata_type"
    legacyJson.stringify
  }
}