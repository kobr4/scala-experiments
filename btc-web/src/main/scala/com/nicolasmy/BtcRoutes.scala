package com.nicolasmy

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.http.scaladsl.model.{ContentTypes, StatusCodes}
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.ActorMaterializer
import akka.http.scaladsl.model.headers.`Content-Type`

import scala.concurrent.ExecutionContext

trait BtcRoutes {
  // we leave these abstract, since they will be provided by the App
  implicit def system: ActorSystem

  implicit def am: ActorMaterializer

  private lazy val log = Logging(system, classOf[BtcRoutes])

  implicit def ec: ExecutionContext

  private lazy val client = new BitcoinRPCClient()(system, am, ec)

  lazy val btcRoutes: Route =
    pathPrefix("btc-api") {
      pathPrefix("public") {
        getFromResourceDirectory("public")
      } ~ pathPrefix("api") {
        getFromResource("public/api.html")
      } ~ path("getblockchaininfo") {
        get {
          onSuccess(client.getBlockchainInfo()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getnetworkinfo") {
        get {
          onSuccess(client.getNetworkInfo()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getmempoolinfo") {
        get {
          onSuccess(client.getMempoolInfo()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getpeerinfo") {
        get {
          onSuccess(client.getPeerInfo()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getnettotals") {
        get {
          onSuccess(client.getNetTotals()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getblockcount") {
        get {
          onSuccess(client.getBlockCount()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getrawmempool") {
        get {
          onSuccess(client.getRawMemPool()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getmemoryinfo") {
        get {
          onSuccess(client.getMemoryInfo()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getdifficulty") {
        get {
          onSuccess(client.getDifficulty()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("getrawtransaction") {
        get {
          parameters("TXID", 'verbose.as[Boolean]) { (txid, verbose) =>
            onSuccess(client.getRawTransaction(txid, verbose)) {
              complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
            }
          }
        }
      } ~ path("getblockhash") {
        get {
          parameters('height.as[Long]) { (height) =>
            onSuccess(client.getBlockHash(height)) {
              complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
            }
          }
        }
      } ~ path("getblock") {
        get {
          parameters("blockhash", 'verbosity.as[Long]) { (blockhash, verbosity) =>
            onSuccess(client.getBlock(blockhash, verbosity)) {
              complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
            }
          }
        }
      } ~ path("getchaintips") {
        get {
          onSuccess(client.getChainTips()) {
            complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
          }
        }
      } ~ path("help") {
        get {
          parameters("method".?) { (method) =>
            onSuccess(client.help(method)) {
              complete(StatusCodes.OK, List(`Content-Type`(ContentTypes.`application/json`)), _)
            }
          }
        }
      }
    }
}
