package com.kobr4.tradebot.db

import slick.jdbc.MySQLProfile.api._
import slick.lifted.Tag

import scala.concurrent.{ ExecutionContext, Future }

case class ApiKey(id: Int, userId: Int, exchange: String, key: String, secret: String)

class ApiKeysRepository(dbConfigPath: String) {

  val db = Database.forConfig(dbConfigPath)

  def insertApiKey(apiKey: ApiKey): Future[Int] = {
    db.run((ApiKeysRepository.apiKeys returning ApiKeysRepository.apiKeys.map(_.id)) += apiKey)
  }

  def selectApiLKeyByUserId(userId: Int)(implicit ec: ExecutionContext): Future[Seq[ApiKey]] = {
    db.run(ApiKeysRepository.apiKeys.filter(_.userId === userId).result)
  }

  def selectApiLKeyById(id: Int)(implicit ec: ExecutionContext): Future[Option[ApiKey]] = {
    db.run(ApiKeysRepository.apiKeys.filter(_.id === id).result).map(_.headOption)
  }

  def updateApiKey(apiKey: ApiKey): Future[Int] = {
    db.run((for { a <- ApiKeysRepository.apiKeys if a.id === apiKey.id } yield a).update(apiKey))
  }

  def deleteApiKey(id: Int): Future[Int] = {
    db.run((for { a <- ApiKeysRepository.apiKeys if a.id === id } yield a).delete)
  }
}

object ApiKeysRepository {

  class ApiKeys(tag: Tag) extends Table[ApiKey](tag, "api_keys") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def exchange = column[String]("exchange")
    def key = column[String]("key")
    def secret = column[String]("secret")

    def * = (id, userId, exchange, key, secret) <> (ApiKey.tupled, ApiKey.unapply)
  }

  val apiKeys = TableQuery[ApiKeys]
}