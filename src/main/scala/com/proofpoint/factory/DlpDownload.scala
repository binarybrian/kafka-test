package com.proofpoint
package factory

import java.util.UUID

import play.api.libs.json.{JsValue, Json, Writes}

case class DlpDownload(bucketName: String, filename: String, filesize: Int, tenantId: String = "tenant_99da1321d75f41d4935ffffcd16593c6") {
  def s3Path: String = s"s3://$bucketName/$filename"

  def s3PathPublic: String = s"https://s3.amazonaws.com/$bucketName/$filename"
}

object DlpDownload {
  implicit val dlpDownloadWrites: Writes[DlpDownload] = new Writes[DlpDownload] {
    override def writes(dlpDownload: DlpDownload): JsValue = {
      Json.obj(
        "so_id" -> UUID.randomUUID().toString,
        "s3_bucket_name" -> dlpDownload.bucketName,
        "s3_file_name" -> dlpDownload.filename,
        "s3_download_url" -> dlpDownload.s3Path,
        "event_id" -> "execution_plan_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09 job_id: cap_job_c2918889f673dddca55411425991fe09 transaction_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09_1548805779981 entity: 2198f92743bd28872ccd5b29b8643c92",
        "title" -> dlpDownload.filename,
        "extension" -> "zip",
        "size" -> dlpDownload.filesize.toString,
        "link" -> "https://app.box.com/file/392453406649",
        "location" -> "V11",
        "origin" -> "ONEDRIVE",
        "mimetype" -> "application/zip",
        "trashed" -> "false",
        "share_level" -> 5,
        "owner" -> Json.obj(
          "id" -> "user_d30773b74d7d2457940801e4cc0eb458",
          "name" -> "Omega Helix",
          "email" -> "omegahelix@gmail.com",
          "ip" -> "192.168.1.101",
          "location" -> Json.obj(
            "country" -> "United States",
            "country_iso_code" -> "US",
            "region" -> "California",
            "city" -> "Concord",
          ),
        ),
        "source_metadata" -> Json.obj(
          "channel_type" -> "SaaSFile",
          "id" -> "2198f92743bd28872ccd5b29b8643c92",
          "tenant_id" -> dlpDownload.tenantId,
          "channel_source" -> "PCASB",
          "application_type" -> "Office365",
          "application_specific_metadata" -> Json.obj(
            "owner_id" -> "omegahelix@gmail.com",
            "raw_so_id" -> "7a1ba454-e969-4e61-a83d-b9e99084e3c1",
            "session_id" -> "flevent_549685dfcd78e7d4a0a9a97e4100cc38",
            "event_id" -> "flevent_549685dfcd78e7d4a0a9a97e4100cc38",
            "source" -> "office365native_cap",
            "sub_source" -> "O365",
            "scan_source_type" -> "FILE_EVENT"
          )
        ),
        "source_moniker" -> Json.obj(
          "kloudless_so_id" -> "FIyS49Q0WK2kDIjbPAeYkrw==",
          "kloudless_owner_id" -> "uMTE4MTAyMTQ1Mg==",
          "tenant_id" -> dlpDownload.tenantId,
          "application_id" -> "20",
          "sub_source" -> "O365",
          "file_extension" -> "zip",
        )
      )
    }
  }
}