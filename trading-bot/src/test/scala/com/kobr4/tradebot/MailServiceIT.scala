package com.kobr4.tradebot

import com.kobr4.tradebot.services.MailService
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.ExecutionContext

class MailServiceIT extends FlatSpec with Matchers with ScalaFutures {

  it should "send mail" in {
    implicit val ec = ExecutionContext.global

    MailService.sendMail("my subject", "my body", DefaultConfiguration.Mail.Admin)

    Thread.sleep(1000)
  }

  it should "send an activation mail" in {
    implicit val ec = ExecutionContext.global

    MailService.sendActivationMail(DefaultConfiguration.Mail.Admin)

    Thread.sleep(1000)
  }

}
