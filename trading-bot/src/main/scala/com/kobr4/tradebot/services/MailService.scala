package com.kobr4.tradebot.services

import java.time.ZonedDateTime

import com.kobr4.tradebot.DefaultConfiguration
import com.kobr4.tradebot.model.{ Buy, Order, Sell }
import com.typesafe.scalalogging.StrictLogging
import courier.{ Envelope, Mailer, Multipart, Text }
import javax.mail.internet.InternetAddress

import scala.concurrent.ExecutionContext
import scala.util.Failure
import scalatags.Text.all._

object MailService extends StrictLogging {

  val mailer = Mailer(DefaultConfiguration.Mail.Host, 25).auth(false).startTls(false)()

  def sendMail(subject: String, body: String, to: String)(implicit ec: ExecutionContext): Unit = {

    mailer(Envelope.from(new InternetAddress(DefaultConfiguration.Mail.Sender))
      .to(new InternetAddress(to))
      .subject(subject)
      .content(Multipart().html(body))).onComplete {
      case Failure(f) =>
        logger.error("sending mail failed with error {}", f.getMessage)
        f.printStackTrace()
      case _ => logger.info("message delivered")
    }

  }

  def sendActivationMail(login: String)(implicit ec: ExecutionContext): Unit = {
    AuthService.issueToken(0, login, ZonedDateTime.now().toEpochSecond).foreach(token =>
      sendMail(
        s"[${DefaultConfiguration.Service.Name}] Activation mail",
        activationMail(token), login))
  }

  def orderExecutionMail(to: String, orderList: List[Order])(implicit ec: ExecutionContext): Unit = {
    sendMail(
      s"[${DefaultConfiguration.Service.Name}] Order execution mail",
      orderListMail(orderList),
      to)
  }

  private def activationMail(token: String): String = {
    html(
      body(
        p("Dear new user,"),
        p(s"Welcome to our service ${DefaultConfiguration.Service.Name}"),
        p(
          "The last step is to activate your account: ",
          a(href := s"${DefaultConfiguration.Service.Url}/activation?token=$token")("Follow this link to activate"))))
  }.render

  private def orderListMail(orderList: List[Order]): String = {
    html(
      body(
        p("Dear user,"),
        p("A scheduled job has run"),
        p("The following orders were executed: "),
        orderList.map {
          case b: Buy => b.toHtml
          case s: Sell => s.toHtml
        })).render
  }
}
