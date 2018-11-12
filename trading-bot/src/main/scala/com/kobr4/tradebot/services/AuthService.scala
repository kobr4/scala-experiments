package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import com.kobr4.tradebot.DefaultConfiguration
import play.api.libs.json.{ JsObject, Json }
import pdi.jwt.{ JwtAlgorithm, JwtJson }

import scala.util.Try

case class AppToken(userId: Long, timestamp: Long, claims: List[String])

object AppToken {
  implicit val appTokenFormat = Json.format[AppToken]
}

object AuthService {

  private val algo = JwtAlgorithm.HS256

  def issueToken(login: String, password: String, currentTimestamp: Long = ZonedDateTime.now().toEpochSecond): Option[String] = {
    val json = Json.toJsObject(AppToken(1, currentTimestamp, List("USER")))

    Try(JwtJson.encode(json, DefaultConfiguration.Jwt.Secret, algo)).toOption
  }

  def verifyToken(token: String): Option[AppToken] = {
    JwtJson.decodeJson(token, DefaultConfiguration.Jwt.Secret, Seq(JwtAlgorithm.HS256)).toOption.flatMap { jsObj: JsObject =>
      jsObj.asOpt[AppToken].flatMap { appToken =>
        if (ZonedDateTime.now().toEpochSecond - appToken.timestamp < DefaultConfiguration.Jwt.Expiry) Some(appToken) else None
      }
    }
  }

}
