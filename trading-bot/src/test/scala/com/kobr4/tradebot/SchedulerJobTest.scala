package com.kobr4.tradebot

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.services.SchedulingService
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.util.Success

class SchedulerJobForTest extends SchedulerJobInterface {
  override def run(): Unit = {
    println("test")
  }
}

class SchedulerJobTest extends FlatSpec with Matchers with ScalaFutures with BeforeAndAfterEach {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  it should "instantiate and run a SchedulerJob class" in {

    val service = new SchedulingService()

    val taskConfiguration = ScheduledTaskConfiguration(
      "test-task",
      "com.kobr4.tradebot.SchedulerJobForTest",
      "*/10 * * * * ?", true)

    SchedulerJob.fromConfiguration(service, taskConfiguration) shouldBe Success()
  }
}
