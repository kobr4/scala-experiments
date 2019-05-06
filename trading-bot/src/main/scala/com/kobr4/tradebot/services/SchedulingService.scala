package com.kobr4.tradebot.services

import java.util.{ Date, TimeZone }

import akka.actor.{ Actor, ActorSystem, Props }
import akka.stream.ActorMaterializer
import com.typesafe.akka.extension.quartz.QuartzSchedulerExtension
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.ExecutionContext

class WorkerActor(func: () => Unit) extends Actor {

  def receive = {
    case "run" => func()
  }
}

class SchedulingService(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends StrictLogging {

  val scheduler = QuartzSchedulerExtension(arf)

  def schedule(name: String, cronExpression: String, func: () => Unit): Date = {

    //Fix calls that are not thread-safe
    synchronized(scheduler) {

      logger.info("Create schedule [{}] cron [{}]", name, cronExpression)

      scheduler.createSchedule(name, None, cronExpression, None, TimeZone.getTimeZone("UTC"))

      val actorRef = arf.actorOf(Props(classOf[WorkerActor], func), name)
      scheduler.schedule(name, actorRef, "run", None)

    }
  }

  def listJobs(): List[String] = {
    synchronized(scheduler) {
      scheduler.schedules.keys.toList
    }
  }

  def cancelJob(name: String): Boolean = {
    synchronized(scheduler) {
      scheduler.cancelJob(name)
    }
  }
}
