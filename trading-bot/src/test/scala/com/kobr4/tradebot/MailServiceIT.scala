package com.kobr4.tradebot

import java.time.ZonedDateTime

import com.kobr4.tradebot.api.CurrencyPair
import com.kobr4.tradebot.model.{Asset, Buy}
import com.kobr4.tradebot.services.MailService
import org.scalatest.{FlatSpec, Matchers}
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

  it should "send an order execution mail" in {
    implicit val ec = ExecutionContext.global

    MailService.orderExecutionMail(DefaultConfiguration.Mail.Admin, List(Buy(CurrencyPair(Asset.Usd, Asset.Btc),
      BigDecimal(1.0), BigDecimal(1.0), ZonedDateTime.now())))

    Thread.sleep(1000)
  }

}
