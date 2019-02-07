package com.proofpoint
package factory

import java.util.UUID

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.{JsonSerializer, SerializerProvider}

@JsonSerialize(using = classOf[DlpDownloadSerializer])
case class DlpDownload(filename: String, s3Path: String, filesize: Int)

class DlpDownloadSerializer extends JsonSerializer[DlpDownload] {
  override def serialize(dlpDownload: DlpDownload, generator: JsonGenerator, serializers: SerializerProvider): Unit = {
    generator.writeStartObject()

    generator.writeStringField("so_id", UUID.randomUUID().toString)
    generator.writeStringField("s3_bucket_name", s3BucketName)
    generator.writeStringField("s3_file_name", dlpDownload.filename)
    generator.writeStringField("s3_download_url", dlpDownload.s3Path)
    generator.writeStringField("event_id", "execution_plan_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09 job_id: cap_job_c2918889f673dddca55411425991fe09 transaction_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09_1548805779981 entity: 2198f92743bd28872ccd5b29b8643c92")

    generator.writeObjectFieldStart("source_metadata")
    generator.writeStringField("channel_type", "SaaSFile")
    generator.writeStringField("id", "2198f92743bd28872ccd5b29b8643c92")
    generator.writeStringField("tenant_id", "tenant_99da1321d75f41d4935ffffcd16593c6")
    generator.writeStringField("channel_source", "PCASB")
    generator.writeStringField("application_type", "Office365")

    generator.writeObjectFieldStart("application_specific_metadata")
    generator.writeStringField("owner_id", "omegahelix@gmail.com")
    generator.writeStringField("raw_so_id", "7a1ba454-e969-4e61-a83d-b9e99084e3c1")
    generator.writeStringField("session_id", "flevent_549685dfcd78e7d4a0a9a97e4100cc38")
    generator.writeStringField("event_id", "flevent_549685dfcd78e7d4a0a9a97e4100cc38")
    generator.writeStringField("source", "office365native_cap")
    generator.writeStringField("sub_source", "O365")
    generator.writeStringField("scan_source_type", "FILE_EVENT")
    generator.writeEndObject()

    generator.writeObjectFieldStart("source_moniker")
    generator.writeStringField("kloudless_so_id", "FIyS49Q0WK2kDIjbPAeYkrw==")
    generator.writeStringField("kloudless_owner_id", "uMTE4MTAyMTQ1Mg==")
    generator.writeStringField("tenant_id", "tenant_99da1321d75f41d4935ffffcd16593c6")
    generator.writeStringField("application_id", "20")
    generator.writeStringField("sub_source", "O365")
    generator.writeStringField("file_extension", "zip")
    generator.writeEndObject()

    generator.writeStringField("title", s"${dlpDownload.filename}")
    generator.writeStringField("extension", "zip")
    generator.writeStringField("size", s"${dlpDownload.filesize}")
    generator.writeStringField("link", "https://app.box.com/file/392453406649")
    generator.writeStringField("location", "V11")
    generator.writeStringField("origin", "ONEDRIVE")
    generator.writeStringField("mimetype", "application/zip")
    generator.writeStringField("trashed", "false")

    generator.writeObjectFieldStart("owner")
    generator.writeStringField("id", "user_d30773b74d7d2457940801e4cc0eb458")
    generator.writeStringField("name", "Omega Helix")
    generator.writeStringField("email", "omegahelix@gmail.com")
    generator.writeStringField("ip", "192.168.1.101")

    generator.writeObjectFieldStart("location")
    generator.writeStringField("country", "United States")
    generator.writeStringField("country_iso_code", "US")
    generator.writeStringField("region", "California")
    generator.writeStringField("city", "Concord")
    generator.writeEndObject()

    generator.writeEndObject() //owner

    generator.writeNumberField("share_level", 5)

    generator.writeEndObject() //source_metadata

    generator.writeEndObject()
  }
}