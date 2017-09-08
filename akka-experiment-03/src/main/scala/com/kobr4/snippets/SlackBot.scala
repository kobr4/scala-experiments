package com.kobr4.snippets

import akka.actor.ActorSystem
import com.kobr4.polo.{PoloQuote, User}
import slack.rtm.SlackRtmClient

import scala.collection.immutable.HashMap
import scala.concurrent.{ExecutionContext, Future}
import akka.stream.ActorMaterializer

object SlackBot {

  import scala.concurrent.duration._

  case object InvalidSlackCommandException extends RuntimeException

  def start(token: String)(implicit arf: ActorSystem, materializer: ActorMaterializer, ec: ExecutionContext) = {
    val commandProcessor = HashMap(
      "polo" -> HashMap("quote" -> { (u: String, v: Seq[String]) => PoloQuote.searchQuote(v.head) },
        "portfolio" -> { (u: String, v: Seq[String]) => PoloQuote.createPortfolio(User(u), v.toList) },
        "value" -> { (u: String, v: Seq[String]) => PoloQuote.value(User(u)) }
      )
    )

    def runCommand(user: String, command: Seq[String]): Future[String] = command match {
      case p1 :: p2 :: p3 => commandProcessor.get(p1).flatMap(_.get(p2).map { func => func(user, p3) }) match {
        case Some(f) => f
        case _ => Future.failed(InvalidSlackCommandException)
      }
    }

    val clientRtmSlack = SlackRtmClient(token, 10 seconds)
    clientRtmSlack.onMessage { message =>
      if (message.text.toUpperCase.contains("MYSCALABOT")) {
        message.text.split(" ").toList match {
          case p1 :: p2 => runCommand(message.user, p2) map { reponse => clientRtmSlack.sendMessage(message.channel, reponse) }
          case other => clientRtmSlack.sendMessage(message.channel, "Sorry, I didn't understood you.")
        }
      }
      println(s"User: ${message.user}, Message: ${message.text}")
    }
  }
}
