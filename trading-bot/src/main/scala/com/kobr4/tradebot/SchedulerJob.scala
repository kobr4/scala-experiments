package com.kobr4.tradebot

import com.kobr4.tradebot.services.SchedulingService

import scala.util.Try

trait SchedulerJobInterface {
  def run(): Unit
}

object SchedulerJob {

  def fromConfiguration(service: SchedulingService, taskConfig: ScheduledTaskConfiguration): Try[Unit] = Try {

    val jobInstance = Class.forName(taskConfig.classToCall).getConstructors.head.newInstance().asInstanceOf[SchedulerJobInterface]

    service.schedule(taskConfig.name, taskConfig.cronExpression, () => jobInstance.run())

  }

}
