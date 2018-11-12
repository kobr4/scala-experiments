package com.kobr4.tradebot.services

import java.time.ZonedDateTime

case class User(email: String, password: String, activationDate: Option[ZonedDateTime])


object UserService {

  def signUp(email: String, password: String): User = {
    User(email, password, None)
  }

  def activate(email: String): Unit = {

  }

}
