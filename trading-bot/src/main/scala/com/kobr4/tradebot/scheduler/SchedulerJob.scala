package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.api.{ ExchangeApi, SupportedExchange }
import com.kobr4.tradebot.db.TradingJob
import com.kobr4.tradebot.engine.Strategy
import com.kobr4.tradebot.model.Asset
import com.kobr4.tradebot.services.{ SchedulingService, UserService }
import com.kobr4.tradebot.{ Configuration, ScheduledTaskConfiguration }

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

trait SchedulerJobInterface {
  def run()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Unit
}

object SchedulerJob {

  def fromConfiguration(service: SchedulingService, taskConfig: ScheduledTaskConfiguration)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Try[Unit] = Try {

    val jobInstance = Class.forName(taskConfig.classToCall).getConstructors.head.newInstance().asInstanceOf[SchedulerJobInterface]

    service.schedule(taskConfig.name, taskConfig.cronExpression, () => jobInstance.run())
  }

  def loadConfiguration(config: Configuration, service: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): List[Try[Unit]] = {

    config.Scheduled.tasks.filter(_.enabled).map(SchedulerJob.fromConfiguration(service, _))
  }

  def schedule(job: TradingJob, service: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Unit = {

    UserService.getApiKey(job.apiKeyId).map { maybeApiKey =>
      maybeApiKey.map { apiKey =>
        val jobInstance = new TradeBotDailyJob {

          override def getExchangeInterface()(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): ExchangeApi =
            ExchangeApi(SupportedExchange.fromString(apiKey.exchange), apiKey.key, apiKey.secret)

          override def getStrategy(): Strategy = job.strategy

          override def getAssetMap(): Map[Asset, BigDecimal] = job.weights
        }
        service.schedule(s"${job.id}-${job.userId}-${job.apiKeyId}", "0 0 9 * * ?", () => jobInstance.run())
      }
    }
  }

  def cancel(job: TradingJob, service: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Unit = {

    service.cancelJob(s"${job.id}-${job.userId}-${job.apiKeyId}")
  }

  def fromDB(service: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Unit] = {

    UserService.getAllTradingJobs().map { jobs => jobs.foreach(schedule(_, service)) }
  }

}
