package com.proofpoint
package factory

import java.util.UUID

import play.api.libs.json._

case class DlpDownload(bucketName: String, filename: String, filesize: Int, tenantId: String = "tenant_99da1321d75f41d4935ffffcd16593c6") {
  def s3Path: String = s"s3://$bucketName/$filename"

  def s3PathPublic: String = s"https://s3.amazonaws.com/$bucketName/$filename"
}

object DlpDownload {
  implicit val format: Format[DlpDownload] = Json.format[DlpDownload]

  def writeTestJson(dlpDownload: DlpDownload): JsObject = {
    JsObject(Seq(
      "so_id" -> JsString(UUID.randomUUID().toString),
      "s3_bucket_name" -> JsString(dlpDownload.bucketName),
      "s3_file_name" -> JsString(dlpDownload.filename),
      "s3_download_url" -> JsString(dlpDownload.s3Path),
      "event_id" -> JsString("execution_plan_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09 job_id: cap_job_c2918889f673dddca55411425991fe09 transaction_id: cap_execution_plan_d9d90b80606e4472a138226bb658ed09_1548805779981 entity: 2198f92743bd28872ccd5b29b8643c92"),
      "source_metadata" -> JsObject(
        Seq(
          "channel_type" -> JsString("SaaSFile"),
          "id" -> JsString("2198f92743bd28872ccd5b29b8643c92"),
          "tenant_id" -> JsString(dlpDownload.tenantId),
          "channel_source" -> JsString("PCASB"),
          "application_type" -> JsString("Office365")
        )),
      "application_specific_metadata" -> JsObject(
        Seq(
          "owner_id" -> JsString("omegahelix@gmail.com"),
          "raw_so_id" -> JsString("7a1ba454-e969-4e61-a83d-b9e99084e3c1"),
          "session_id" -> JsString("flevent_549685dfcd78e7d4a0a9a97e4100cc38"),
          "event_id" -> JsString("flevent_549685dfcd78e7d4a0a9a97e4100cc38"),
          "source" -> JsString("office365native_cap"),
          "sub_source" -> JsString("O365"),
          "scan_source_type" -> JsString("FILE_EVENT")
        )),
      "source_moniker" -> JsObject(
        Seq(
          "kloudless_so_id" -> JsString("FIyS49Q0WK2kDIjbPAeYkrw=="),
          "kloudless_owner_id" -> JsString("uMTE4MTAyMTQ1Mg=="),
          "tenant_id" -> JsString(dlpDownload.tenantId),
          "application_id" -> JsString("20"),
          "sub_source" -> JsString("O365"),
          "file_extension" -> JsString("zip"),
        )),
      "title" -> JsString(dlpDownload.filename),
      "extension" -> JsString("zip"),
      "size" -> JsString(dlpDownload.filesize.toString),
      "link" -> JsString("https://app.box.com/file/392453406649"),
      "location" -> JsString("V11"),
      "origin" -> JsString("ONEDRIVE"),
      "mimetype" -> JsString("application/zip"),
      "trashed" -> JsString("false"),
      "owner" -> JsObject(
        Seq(
          "id" -> JsString("user_d30773b74d7d2457940801e4cc0eb458"),
          "name" -> JsString("Omega Helix"),
          "email" -> JsString("omegahelix@gmail.com"),
          "ip" -> JsString("192.168.1.101"),
          "location" -> JsObject(
            Seq(
              "country" -> JsString("United States"),
              "country_iso_code" -> JsString("US"),
              "region" -> JsString("California"),
              "city" -> JsString("Concord"),
            )),
        )),
      "share_level" -> JsNumber(5)
    ))
  }
}