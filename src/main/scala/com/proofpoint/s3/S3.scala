package com.proofpoint.s3

import java.io.{File, InputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.Path
import java.nio.file.StandardOpenOption.READ
import java.util.concurrent.CompletionException

import com.proofpoint.s3.S3.{bufferSize, client}
import javax.inject.Inject
import software.amazon.awssdk.core.async.{AsyncRequestBody, AsyncResponseTransformer}
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.ServerSideEncryption.AWS_KMS
import software.amazon.awssdk.services.s3.model._

import scala.collection.JavaConverters._
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ExecutionContext, Future}

object S3 {
  private val bufferSize = 5 * 1024 * 1024
  private lazy val client = S3AsyncClient.create()
}

class S3 @Inject()(implicit executionContext: ExecutionContext) {
  def buckets: Future[Seq[Bucket]] = {
    client.listBuckets().toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.buckets.asScala)
      .map(_.toSeq)
  }

  def objects(bucketName: String, prefix: Option[String] = None): Future[Seq[S3Object]] = {
    val request = ListObjectsRequest.builder().bucket(bucketName).prefix(prefix.orNull).build()
    client.listObjects(request).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.contents.asScala)
      .map(Option.apply)
      .map(_.map(_.toSeq))
      .map(_.getOrElse(Seq.empty))
  }

  def createBucket(bucketName: String): Future[String] = {
    val request = CreateBucketRequest.builder().bucket(bucketName).build()
    client.createBucket(request).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.location)
  }

  def deleteBucket(bucketName: String): Future[Unit] = {
    val request = DeleteBucketRequest.builder().bucket(bucketName).build()
    client.deleteBucket(request).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }

  def download(bucketName: String, key: String, destinationPath: Path): Future[Unit] = {
    val request = GetObjectRequest.builder().bucket(bucketName).key(key).build()
    client.getObject(request, destinationPath).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }

  def downloadToStream(bucketName: String, key: String): Future[InputStream] = {
    val request = GetObjectRequest.builder().bucket(bucketName).key(key).build()
    client.getObject(request, AsyncResponseTransformer.toBytes[GetObjectResponse]).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.asInputStream())
  }

  def downloadToString(bucketName: String, key: String): Future[String] = {
    val request = GetObjectRequest.builder().bucket(bucketName).key(key).build()
    client.getObject(request, AsyncResponseTransformer.toBytes[GetObjectResponse]).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.asUtf8String())
  }

  def upload(content: String, bucketName: String, key: String): Future[Unit] = {
    val request = PutObjectRequest.builder().bucket(bucketName).key(key).serverSideEncryption(AWS_KMS).build()
    client.putObject(request, AsyncRequestBody.fromString(content)).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }

  def upload(file: File, bucketName: String, key: String): Future[Unit] = {
    if (file.length() <= bufferSize) uploadFile(file, bucketName, key)
    else uploadMultipart(file, bucketName, key)
  }

  private def uploadFile(file: File, bucketName: String, key: String) = {
    val request = PutObjectRequest.builder().bucket(bucketName).key(key).serverSideEncryption(AWS_KMS).build()
    client.putObject(request, AsyncRequestBody.fromFile(file)).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }

  private def uploadMultipart(file: File, bucketName: String, key: String) = {
    createMultipartUpload(bucketName, key)
      .flatMap(uploadFileChunked(file, bucketName, key, _))
  }

  private def createMultipartUpload(bucketName: String, key: String): Future[String] = {
    val request = CreateMultipartUploadRequest.builder().bucket(bucketName).key(key).serverSideEncryption(AWS_KMS).build()
    client.createMultipartUpload(request).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_.uploadId)
  }

  private def uploadFileChunked(file: File, bucketName: String, key: String, uploadId: String) = {
    val parts = chunkFile(file).zip(Stream.from(1))
    Future.sequence(parts.map(uploadPart(bucketName, key, uploadId)))
      .flatMap(completeMultipartUpload(bucketName, key, uploadId))
      .recover {
        case e =>
          client.abortMultipartUpload(builder => builder.bucket(bucketName).key(key).uploadId(uploadId))
          throw e
      }
  }

  def chunkFile(file: File): Seq[ByteBuffer] = {
    val fileChannel = FileChannel.open(file.toPath, READ)
    try {
      val fullBuffers = (file.length() / bufferSize).intValue()
      val partialBuffer = (file.length() % bufferSize).intValue()
      val buffers = Seq.fill(fullBuffers)(ByteBuffer.allocate(bufferSize)) :+ ByteBuffer.allocate(partialBuffer)
      fileChannel.read(buffers.toArray)
      buffers
    }
    finally {
      fileChannel.close()
    }
  }

  private def uploadPart(bucketName: String, key: String, uploadId: String)(part: (ByteBuffer, Int)): Future[CompletedPart] = {
    val (buffer, partNumber) = part
    val request = UploadPartRequest.builder().bucket(bucketName).key(key).uploadId(uploadId).partNumber(partNumber).build()
    client.uploadPart(request, AsyncRequestBody.fromByteBuffer(buffer)).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(response => CompletedPart.builder().partNumber(partNumber).eTag(response.eTag).build())
  }

  private def completeMultipartUpload(bucketName: String, key: String, uploadId: String)(completedParts: Seq[CompletedPart]): Future[Unit] = {
    val completedUpload = CompletedMultipartUpload.builder().parts(completedParts.asJava).build()
    val completedRequest = CompleteMultipartUploadRequest.builder().bucket(bucketName).key(key).uploadId(uploadId).multipartUpload(completedUpload).build()
    client.completeMultipartUpload(completedRequest).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }

  def deleteObject(bucketName: String, filename: String): Future[Unit] = {
    val request = DeleteObjectRequest.builder().bucket(bucketName).key(filename).build()
    client.deleteObject(request).toScala
      .recover {
        case e: CompletionException => throw e.getCause
      }
      .map(_ => ())
  }
}

