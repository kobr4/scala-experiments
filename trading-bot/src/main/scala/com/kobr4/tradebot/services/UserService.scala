package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import com.kobr4.tradebot.db.UsersRepository
import com.typesafe.scalalogging.StrictLogging
import com.github.t3hnar.bcrypt._
import scala.concurrent.{ ExecutionContext, Future }

case class User(id: Option[Int], email: String, password: String, activationDate: Option[ZonedDateTime], created: ZonedDateTime)

object UserService extends StrictLogging {

  val usersRepository = new UsersRepository("mysql")

  def signUp(email: String, password: String)(implicit ec: ExecutionContext): Future[Option[Int]] = {
    usersRepository.selectUserByEmail(email).flatMap {
      case Some(_) =>
        logger.info("User {} already exists in DB", email)
        Future.successful(None)
      case _ =>
        logger.info("Will insert new user {} in DB", email)
        usersRepository.insertUser(User(None, email, password.bcrypt, None, ZonedDateTime.now())).map { i =>
          logger.info("Sending mail to user {}", email)
          MailService.sendActivationMail(email)
          i
        }
    }
  }

  def activate(email: String)(implicit ec: ExecutionContext): Future[Int] = {
    usersRepository.selectUserByEmail(email).flatMap {
      case Some(user) =>
        val activatedUser = User(user.id, user.email, user.password, Some(ZonedDateTime.now()), user.created)
        usersRepository.updateUser(activatedUser)
      case _ => Future.successful(0)
    }
  }

  def get(email: String)(implicit ec: ExecutionContext): Future[Option[User]] = {
    usersRepository.selectUserByEmail(email)
  }

  def verify(email: String, password: String)(implicit ec: ExecutionContext): Future[Boolean] = {
    get(email).map {
      case Some(s) => password.isBcryptedSafe(s.password).getOrElse(false)
      case _ => false
    }
  }

}
