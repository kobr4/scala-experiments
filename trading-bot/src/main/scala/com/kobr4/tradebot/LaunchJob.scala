package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.scheduler.{ KrakenDailyJob, TradeBotDailyJob }

import scala.concurrent.ExecutionContext

object LaunchJob {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    //val job = new TradeBotDailyJob()
    val job = new KrakenDailyJob()

    job.run()

  }

}
