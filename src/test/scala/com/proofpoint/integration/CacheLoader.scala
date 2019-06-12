package com.proofpoint.integration

import java.util.concurrent.{CompletableFuture, Executor}

import com.github.benmanes.caffeine.cache.{AsyncCacheLoader, AsyncLoadingCache, Caffeine}
import com.github.blemale.scaffeine.Scaffeine
import com.proofpoint.commons.logging.{Logging, LoggingContext, SimpleLoggingContext}

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future, Promise}
import scala.util.{Failure, Success, Try}

object MockAuthenticationApp extends App {
  var i = 0

  protected val authenticationCache: AsyncLoadingCache[String, String] = Caffeine.newBuilder()
    //.refreshAfterWrite(1, MINUTES)
    //.expireAfterWrite(1, HOURS)
    .buildAsync(new AsyncCacheLoader[String, String] with Logging {
    override def asyncLoad(key: String, executor: Executor): CompletableFuture[String] = {
      //implicit val executionContext: ExecutionContext = ExecutionContext.fromExecutor(executor)
      implicit val loggingContext: LoggingContext = new SimpleLoggingContext(key.toString)
      val future = new CompletableFuture[String]()
      authenticate().onComplete {
        case Success(authenticationToken) => future.complete(authenticationToken)
        case Failure(e) =>
          //logger.error("Failed to load authentication token", e)
          future.complete(null)
      }
      future
    }
  })


  def authenticate(): Future[String] = {
    println("AUTHENTICATING!!!!")
    i = i + 1
    if (i < 5) Future.failed(new Exception("Failed"))
    else Future.successful("Success")
  }

  (1 to 100).foreach {
    i => {
      val authenticationToken = Try(Await.result(authenticationCache.get("").toScala, Duration.Inf)).toOption
      println(s"$i --> $authenticationToken")
      Thread.sleep(500)
    }
  }

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}

object MockScaffeineApp extends App {
  var i = 0

  def authenticate(): Future[String] = {
    println("AUTHENTICATING!!!!")
    i = i + 1
    if (i < 5) Future.failed(new Exception("Failed"))
    else Future.successful("Success")
  }

  private val authenticationCache: com.github.blemale.scaffeine.AsyncLoadingCache[Unit, String] =
    Scaffeine()
      //.refreshAfterWrite(Duration(2, "seconds"))
      //.expireAfterWrite(Duration(10, "seconds"))
      .maximumSize(1)
      .buildAsyncFuture((_: Unit) => authenticate().recover {
        case _ => null
      })

  (1 to 100).foreach {
    i => {
      val authenticationToken = Try(Await.result(authenticationCache.get(()), Duration.Inf)).toOption
      println(s"$i --> $authenticationToken")
      Thread.sleep(500)
    }
  }

  val p = Promise[String]()
  val f = p.future
  Await.result(f, Duration.Inf)
}
