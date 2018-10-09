package com.kobr4.tradebot.scheduler

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.{ Configuration, ScheduledTaskConfiguration }
import com.kobr4.tradebot.services.SchedulingService

import scala.concurrent.ExecutionContext
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

}
