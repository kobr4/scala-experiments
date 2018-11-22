package com.kobr4.tradebot.db

import com.kobr4.tradebot.engine.AggregatedStrategy
import com.kobr4.tradebot.model.Asset
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ TableQuery, Tag }

import scala.concurrent.{ ExecutionContext, Future }

case class TradingJob(id: Int, userId: Int, cron: String, apiKeyId: Int, strategy: AggregatedStrategy, weights: Map[Asset, BigDecimal], baseAsset: Asset)

object TradingJob {

  implicit val mapReads: Reads[Map[Asset, BigDecimal]] = (jv: JsValue) => JsSuccess(jv.as[Map[String, BigDecimal]].map {
    case (k, v) =>
      Asset.fromString(k) -> v
  })

  implicit val mapWrites: Writes[Map[Asset, BigDecimal]] = (map: Map[Asset, BigDecimal]) => Json.obj(map.map {
    case (s, o) =>
      val ret: (String, JsValueWrapper) = s.toString -> JsNumber(o)
      ret
  }.toSeq: _*)

  def customTupled(a: (Int, Int, String, Int, String, String, String)): TradingJob = {
    TradingJob(a._1, a._2, a._3, a._4, Json.parse(a._5).as[AggregatedStrategy], Json.parse(a._6).as[Map[Asset, BigDecimal]], Asset.fromString(a._7))
  }

  def customUnapply(job: TradingJob): Option[(Int, Int, String, Int, String, String, String)] =
    Option((job.id, job.userId, job.cron, job.apiKeyId, Json.toJson(job.strategy).toString(), Json.toJson(job.weights).toString(), job.baseAsset.toString))
}

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

  def selectTradingJobs()(implicit ec: ExecutionContext): Future[Seq[TradingJob]] = {
    db.run(TradingJobsRepository.tradingJobs.result)
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

    def weights = column[String]("weights")

    def baseAsset = column[String]("base_asset")

    def * = (id, userId, cron, apiKeyId, strategy, weights, baseAsset) <> (TradingJob.customTupled, TradingJob.customUnapply)
  }

  val tradingJobs = TableQuery[TradingJobs]
}
