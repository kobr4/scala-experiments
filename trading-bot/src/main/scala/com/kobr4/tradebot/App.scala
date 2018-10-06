package com.kobr4.tradebot

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.kobr4.tradebot.engine.{ SafeStrategy, Strategy }
import com.kobr4.tradebot.model.{ Asset, Buy, Sell }
import com.kobr4.tradebot.services.{ PriceService, TradeBotService }

import scala.concurrent.ExecutionContext

object App {

  def main(args: Array[String]): Unit = {

    implicit val system: ActorSystem = ActorSystem("helloAkkaHttpServer")
    implicit val am: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContext = system.dispatcher

    /*
    val priceData = PairPrice.fromUrl(ethPricesUrl)
    priceData.prices.filter(_.date.isAfter(date)).foreach { p =>
      Strategy.runStrategy(p.date, priceData)
    }

    val holdBalance = 10000 / priceData.currentPrice(date) * priceData.prices.last.price
    println(s"balance: ${Strategy.portfolio.balance(priceData)} hold: $holdBalance")
*/
    val date = ZonedDateTime.parse("2017-01-01T01:00:00.000Z")

    PriceService.getPriceData(Asset.Btc, date).map { pd =>
      val orderList = TradeBotService.run(Asset.Btc, BigDecimal(10000), pd, BigDecimal(0.1), SafeStrategy)
      val lastOrder = orderList.last._2
      val (asset, price, quantity) = lastOrder match {
        case Buy(asset, price, quantity) => (asset, price, quantity)
        case Sell(asset, price, quantity) => (asset, price, quantity)
      }

      println(s"balance:" + (price * quantity))
    }
  }
}