package com.kobr4.tradebot.routes

import akka.actor.ActorSystem
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.db.{ ApiKey, TradingJob }
import com.kobr4.tradebot.services.{ AppToken, AuthService, SchedulingService, UserService }
import play.api.libs.json.{ Format, Json }

import scala.concurrent.ExecutionContext

object AuthenticationToken extends Directive1[AppToken] {
  def tapply(f: Tuple1[AppToken] => Route): Route = { ctx =>
    val maybeToken = AuthService.verifyToken(ctx.request.headers.filter(_.name() == "Authorization").head.value())
    maybeToken match {
      case Some(token) => f(Tuple1(token))(ctx)
      case None => reject(ctx)
    }
  }
}

trait TradeJobsRoutes extends PlayJsonSupport {

  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  implicit def ec: ExecutionContext

  implicit val tradingJobFormat: Format[TradingJob] = Json.format[TradingJob]

  implicit val apKeyFormat: Format[ApiKey] = Json.format[ApiKey]

  val schedulingService: SchedulingService

  lazy val tradeJobsRoutes: Route = pathPrefix("trading_job") {
    AuthenticationToken { token =>
      path("api_key_get_all") {
        post {
          onSuccess(UserService.getApiKeys(token.id)) { result =>
            complete(result)
          }
        }
      } ~ path("api_key_add") {
        post {
          entity(as[ApiKey]) { apiKey =>
            val newApiKey = ApiKey(0, token.id, apiKey.exchange, apiKey.key, apiKey.secret)
            onSuccess(UserService.addApiKeys(newApiKey)) { result =>
              complete(result)
            }
          }
        }
      } ~ path("api_key_update") {
        post {
          entity(as[ApiKey]) { apiKey =>
            authorize(token.id == apiKey.userId) {
              onSuccess(UserService.updateApiKey(apiKey)) { result =>
                complete(result)
              }
            }
          }
        }
      } ~ path("api_key_delete") {
        post {
          entity(as[ApiKey]) { apiKey =>
            authorizeAsync(UserService.getApiKey(apiKey.id).map(dbApiK => dbApiK.exists(_.userId == token.id))) {
              onSuccess(UserService.deleteApiKey(apiKey.id)) { result =>
                complete(result)
              }
            }
          }
        }
      } ~ path("trade_job_get_all") {
        post {
          onSuccess(UserService.getTradingJobs(token.id)) { result =>
            complete(result)
          }
        }
      } ~ path("trade_job_add") {
        post {
          entity(as[TradingJob]) { tradingJob =>
            authorizeAsync(UserService.getApiKey(tradingJob.apiKeyId).map(dbApiK => dbApiK.exists(_.userId == token.id))) {
              val newTradingJob = TradingJob(0, token.id, tradingJob.cron, tradingJob.apiKeyId, tradingJob.strategy)
              onSuccess(UserService.addTradingJob(newTradingJob, schedulingService)) { result =>
                complete(result)
              }
            }
          }
        }
      } ~ path("trade_job_delete") {
        post {
          entity(as[TradingJob]) { tradeJob =>
            authorizeAsync(UserService.getTradingJob(tradeJob.id).map(dbApiK => dbApiK.exists(_.userId == token.id))) {
              onSuccess(UserService.deleteTradingJob(tradeJob.id, schedulingService)) { result =>
                complete(result)
              }
            }
          }
        }
      } ~ path("trade_job_update") {
        post {
          entity(as[TradingJob]) { tradingJob =>
            authorize(token.id == tradingJob.userId) {
              onSuccess(UserService.updateTradingJob(tradingJob)) { result =>
                complete(result)
              }
            }
          }
        }
      }
    }
  }
}
