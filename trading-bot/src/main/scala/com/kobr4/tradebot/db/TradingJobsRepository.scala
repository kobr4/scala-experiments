package com.kobr4.tradebot.db

import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ TableQuery, Tag }

import scala.concurrent.{ ExecutionContext, Future }

case class TradingJob(id: Int, userId: Int, cron: String, apiKeyId: Int, strategy: String)

class TradingJobsRepository(dbConfigPath: String) {

  val db = Database.forConfig(dbConfigPath)

  def insertTradingJob(tradingJob: TradingJob): Future[Option[Int]] = {
    db.run(TradingJobsRepository.tradingJobs ++= Seq(tradingJob))
  }

  def selectTradingJobByUserId(userId: Int)(implicit ec: ExecutionContext): Future[Seq[TradingJob]] = {
    db.run(TradingJobsRepository.tradingJobs.filter(_.userId === userId).result)
  }

  def selectTradingJobById(id: Int)(implicit ec: ExecutionContext): Future[Option[TradingJob]] = {
    db.run(TradingJobsRepository.tradingJobs.filter(_.id === id).result).map(_.headOption)
  }

  def updateTradingJob(tradingJob: TradingJob): Future[Int] = {
    db.run((for { a <- TradingJobsRepository.tradingJobs if a.id === tradingJob.id } yield a).update(tradingJob))
  }

  def deleteTradingJob(id: Int): Future[Int] = {
    db.run((for { a <- TradingJobsRepository.tradingJobs if a.id === id } yield a).delete)
  }
}

object TradingJobsRepository {

  class TradingJobs(tag: Tag) extends Table[TradingJob](tag, "trading_jobs") {
    def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def cron = column[String]("cron")
    def apiKeyId = column[Int]("api_key_id")
    def strategy = column[String]("strategy")

    def * = (id, userId, cron, apiKeyId, strategy) <> (TradingJob.tupled, TradingJob.unapply)
  }

  val tradingJobs = TableQuery[TradingJobs]
}
