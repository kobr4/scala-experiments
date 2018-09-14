package com.nicolasmy

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json._

import scala.concurrent.{ ExecutionContext, Future }

case class BlockchainInfo(chain: String, blocks: Long, headers: Long, bestblockhash: String, difficulty: BigDecimal)

trait BitcoinAPI {

  def getBlockchainInfo: Future[String]

  def getNetworkInfo: Future[String]

  def getMempoolInfo: Future[String]

  def getPeerInfo: Future[String]

  def getNetTotals: Future[String]
}

object HttpSender extends StrictLogging {

  def httpRequestPost(url: String, body: String)(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext): Future[Option[String]] = {
    logger.info(s"Sending post request to $url")
    val authorization = headers.Authorization(BasicHttpCredentials(DefaultConfiguration.RpcUser, DefaultConfiguration.RpcPassword))
    Http().singleRequest(HttpRequest(
      method = HttpMethods.POST,
      entity = HttpEntity(ContentTypes.`application/json`, body),
      headers = List(authorization),
      uri = url)).flatMap { response =>
      if (response.status == StatusCodes.OK) {
        logger.info("Response OK : " + Unmarshal(response.entity).to[String])
        Unmarshal(response.entity).to[String].map(Some(_))
      } else {
        logger.error("Error code was " + response.status)
        println("Error code was " + response.status)
        println(DefaultConfiguration.RpcPassword)
        Future(None)
      }
    }
  }
}

class BitcoinRPCClient(implicit arf: ActorSystem, am: ActorMaterializer, ec: ExecutionContext) extends BitcoinAPI {

  private implicit val BlockchainInfoReads: Reads[BlockchainInfo] = Json.reads[BlockchainInfo]

  private def httpCall(body: String): Future[String] = {
    HttpSender.httpRequestPost(DefaultConfiguration.RpcUrl, body).map { maybeResponse =>
      maybeResponse.getOrElse(throw new RuntimeException("Unable to parse response"))
    }
  }

  override def getBlockchainInfo(): Future[String] = httpCall("{ \"method\": \"getblockchaininfo\" }")

  override def getNetworkInfo(): Future[String] = httpCall("{ \"method\": \"getnetworkinfo\" }")

  override def getMempoolInfo(): Future[String] = httpCall("{ \"method\": \"getmempoolinfo\" }")

  override def getPeerInfo(): Future[String] = httpCall("{ \"method\": \"getpeerinfo\" }")

  override def getNetTotals(): Future[String] = httpCall("{ \"method\": \"getnettotals\" }")

}
