package com.nicolasmy

import akka.actor.{ ActorRef, ActorSystem }
import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.pathPrefix
import akka.http.scaladsl.server._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import akka.stream.ActorMaterializer

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
            complete(_)
          }
        }
      } ~ path("getnetworkinfo") {
        get {
          onSuccess(client.getNetworkInfo()) {
            complete(_)
          }
        }
      } ~ path("getmempoolinfo") {
        get {
          onSuccess(client.getMempoolInfo()) {
            complete(_)
          }
        }
      } ~ path("getpeerinfo") {
        get {
          onSuccess(client.getPeerInfo()) {
            complete(_)
          }
        }
      } ~ path("getnettotals") {
        get {
          onSuccess(client.getNetTotals()) {
            complete(_)
          }
        }
      } ~ path("getblockcount") {
        get {
          onSuccess(client.getBlockCount()) {
            complete(_)
          }
        }
      } ~ path("getrawmempool") {
        get {
          onSuccess(client.getRawMemPool()) {
            complete(_)
          }
        }
      } ~ path("getrawtransaction") {
        get {
          parameters("TXID", 'verbose.as[Boolean]) { (txid, verbose) =>
            onSuccess(client.getRawTransaction(txid, verbose)) {
              complete(_)
            }
          }
        }
      }
    }
}
