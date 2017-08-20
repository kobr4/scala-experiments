package com.kobr4.polo

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class PairPrice(
                      name: String,
                     last: String,
                     lowestAsk: String,
                     highestBid: String,
                     percentChange: String,
                     baseVolume: String,
                     quoteVolume: String,
                    )


object PairPrice {
  implicit def pairPriceReads(implicit name : String): Reads[PairPrice] = {
    (
      Reads.pure(name) and
        (JsPath \ "last").read[String] and
        (JsPath \ "lowestAsk").read[String] and
        (JsPath \ "highestBid").read[String] and
        (JsPath \ "percentChange").read[String] and
        (JsPath \ "baseVolume").read[String] and
        (JsPath \ "quoteVolume").read[String]
      ) (PairPrice.apply _)
  }


}