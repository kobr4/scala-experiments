package com.kobr4.tradebot

import java.time.ZonedDateTime

import com.kobr4.tradebot.services.AuthService
import org.scalatest.{FlatSpec, Matchers}

class AuthServiceTest extends FlatSpec with Matchers {

  it should "issue a token and decode it" in {

    val token = AuthService.issueToken("login", "password").get

    println(token)

    val appToken = AuthService.verifyToken(token).get

    println(appToken)

  }

  it should "not verify a token when timestamp is expired" in {

    val token = AuthService.issueToken("login", "password", ZonedDateTime.now().toEpochSecond - DefaultConfiguration.Jwt.Expiry - 10).get

    AuthService.verifyToken(token) should be(None)
  }

}
