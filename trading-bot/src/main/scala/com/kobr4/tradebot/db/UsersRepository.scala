package com.kobr4.tradebot.db

import java.sql.Timestamp
import java.time.{ ZoneOffset, ZonedDateTime }

import com.kobr4.tradebot.services.User
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ TableQuery, Tag }

import scala.concurrent.{ ExecutionContext, Future }

class UsersRepository(dbConfigPath: String) {

  val db = Database.forConfig(dbConfigPath)

  def insertUser(user: User): Future[Option[Int]] = {
    db.run(UsersRepository.users ++= Seq(user))
  }

  def selectUserByEmail(email: String)(implicit ec: ExecutionContext): Future[Option[User]] = {
    db.run(UsersRepository.users.filter(_.email === email).result).map(_.headOption)
  }

  def updateUser(user: User): Future[Int] = {
    db.run((for { u <- UsersRepository.users if u.email === user.email } yield u).update(user))
  }
}

object UsersRepository {

  implicit val JavaZonedDateTimeMapper = MappedColumnType.base[ZonedDateTime, Timestamp](
    l => Timestamp.from(l.toInstant),
    t => ZonedDateTime.ofInstant(t.toInstant, ZoneOffset.UTC))

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("email")
    def password = column[String]("password")
    def activation = column[Option[ZonedDateTime]]("activation")
    def created = column[ZonedDateTime]("created")
    def * = (id, email, password, activation, created) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

}

