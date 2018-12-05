package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.t3hnar.bcrypt._
import com.kobr4.tradebot.db._
import com.kobr4.tradebot.scheduler.{ SchedulerJob, TradeBotDailyJob }
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.{ ExecutionContext, Future }

case class User(id: Int, email: String, password: String, activationDate: Option[ZonedDateTime], created: ZonedDateTime)

object UserService extends StrictLogging {

  private val usersRepository = new UsersRepository("mysql")

  private val apiKeysRepository = new ApiKeysRepository("mysql")

  private val tradingJobsRepository = new TradingJobsRepository("mysql")

  def signUp(email: String, password: String)(implicit ec: ExecutionContext): Future[Option[Int]] = {
    usersRepository.selectUserByEmail(email).flatMap {
      case Some(_) =>
        logger.info("User {} already exists in DB", email)
        Future.successful(None)
      case _ =>
        logger.info("Will insert new user {} in DB", email)
        usersRepository.insertUser(User(0, email, password.bcrypt, None, ZonedDateTime.now())).map { i =>
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

  def get(id: Int)(implicit ec: ExecutionContext): Future[Option[User]] = {
    usersRepository.selectUserById(id)
  }

  def verify(email: String, password: String)(implicit ec: ExecutionContext): Future[Option[Int]] = {
    get(email).map {
      case Some(s) => password.isBcryptedSafe(s.password).toOption.flatMap {
        case true => Some(s.id)
        case _ => None
      }
      case _ => None
    }
  }

  def addApiKeys(apiKey: ApiKey)(implicit ec: ExecutionContext): Future[Option[Int]] = {
    apiKeysRepository.insertApiKey(apiKey)
  }

  def getApiKeys(userId: Int)(implicit ec: ExecutionContext): Future[Seq[ApiKey]] = {
    apiKeysRepository.selectApiLKeyByUserId(userId)
  }

  def getApiKey(id: Int)(implicit ec: ExecutionContext): Future[Option[ApiKey]] = {
    apiKeysRepository.selectApiLKeyById(id)
  }

  def deleteApiKey(id: Int)(implicit ec: ExecutionContext): Future[Int] = {
    apiKeysRepository.deleteApiKey(id)
  }

  def updateApiKey(apiKey: ApiKey)(implicit ec: ExecutionContext): Future[Int] = {
    apiKeysRepository.updateApiKey(apiKey)
  }

  def addTradingJob(tradingJob: TradingJob, schedulingService: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Option[Int]] = {
    tradingJobsRepository.insertTradingJob(tradingJob).map { result =>
      result.foreach { id =>
        val insertedTradingJob = TradingJob(id, tradingJob.userId, tradingJob.cron, tradingJob.apiKeyId,
          tradingJob.strategy, tradingJob.weights, tradingJob.baseAsset)
        SchedulerJob.schedule(insertedTradingJob, schedulingService)
      }
      result
    }
  }

  def getTradingJobs(userId: Int)(implicit ec: ExecutionContext): Future[Seq[TradingJob]] = {
    tradingJobsRepository.selectTradingJobByUserId(userId)
  }

  def getTradingJob(id: Int)(implicit ec: ExecutionContext): Future[Option[TradingJob]] = {
    tradingJobsRepository.selectTradingJobById(id)
  }

  def deleteTradingJob(id: Int, schedulingService: SchedulingService)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Int] = {
    for {
      _ <- getTradingJob(id).map(maybeJob => maybeJob.foreach(SchedulerJob.cancel(_, schedulingService)))
      result <- tradingJobsRepository.deleteTradingJob(id)
    } yield {
      result
    }

  }

  def updateTradingJob(tradingJob: TradingJob)(implicit ec: ExecutionContext): Future[Int] = {
    //TODO: Missing rescheduling
    tradingJobsRepository.updateTradingJob(tradingJob)
  }

  def getAllTradingJobs()(implicit ec: ExecutionContext): Future[Seq[TradingJob]] = {
    tradingJobsRepository.selectTradingJobs()
  }
}
