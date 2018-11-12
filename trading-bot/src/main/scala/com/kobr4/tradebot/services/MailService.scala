package com.kobr4.tradebot.services

import com.kobr4.tradebot.DefaultConfiguration
import com.typesafe.scalalogging.StrictLogging
import courier.{ Envelope, Mailer, Text }
import javax.mail.internet.InternetAddress

import scala.concurrent.ExecutionContext
import scala.util.Failure

object MailService extends StrictLogging {

  val mailer = Mailer(DefaultConfiguration.Mail.Host, 25).auth(false).startTls(false)()

  def sendMail(subject: String, body: String, to: String)(implicit ec: ExecutionContext): Unit = {

    mailer(Envelope.from(new InternetAddress(DefaultConfiguration.Mail.Sender))
      .to(new InternetAddress(to))
      .subject(subject)
      .content(Text(body))).onComplete {
      case Failure(f) => logger.error("sending mail failed with error {}", f.getMessage)
        f.printStackTrace()
      case _ => logger.info("message delivered")
    }

  }
}
