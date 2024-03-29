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

  def getBlockCount: Future[String]

  def getRawMemPool: Future[String]

  def getRawTransaction(txid: String, verbose: Boolean): Future[String]

  def getMemoryInfo: Future[String]

  def getDifficulty: Future[String]

  @deprecated
  def estimateFee(blocks: Int): Future[String]

  def getChainTips: Future[String]

  def getBlockHash(height: Long): Future[String]

  def getBlock(blockhash: String, height: Long): Future[String]

  def help(method: Option[String]): Future[String]
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
        Unmarshal(response.entity).to[String].map(Some(_))
      } else {
        logger.error("Error code was " + response.status)
        Unmarshal(response.entity).to[String].map(logger.error(_))
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

  override def getBlockCount(): Future[String] = httpCall("{ \"method\": \"getblockcount\" }")

  override def getRawMemPool(): Future[String] = httpCall("{ \"method\": \"getrawmempool\" }")

  override def getRawTransaction(txid: String, verbose: Boolean): Future[String] =
    httpCall(s"""{ "method": "getrawtransaction", "params" : [ "$txid" , $verbose ] }""")

  override def getMemoryInfo(): Future[String] = httpCall("{ \"method\": \"getmemoryinfo\" }")

  override def getDifficulty(): Future[String] = httpCall("{ \"method\": \"getdifficulty\" }")

  override def estimateFee(blocks: Int): Future[String] =
    httpCall(s"""{ "method": "estimatefee", "params" : [ $blocks ] }""")

  override def getChainTips(): Future[String] = httpCall("{ \"method\": \"getchaintips\" }")

  override def getBlockHash(height: Long): Future[String] = httpCall(s"""{ "method": "getblockhash", "params":[ $height] }""")

  override def getBlock(blockhash: String, height: Long): Future[String] = httpCall(s"""{ "method": "getblock", "params":[ "$blockhash", $height] }""")

  override def help(method: Option[String] = None): Future[String] = httpCall(s"""{ "method": "help", "params":[ "${method.getOrElse("")}"] }""")
}
