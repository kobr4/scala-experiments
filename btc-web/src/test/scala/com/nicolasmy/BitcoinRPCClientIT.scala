package com.nicolasmy

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.{ FlatSpec, Ignore, Matchers }
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

//@Ignore
class BitcoinRPCClientIT extends FlatSpec with Matchers with ScalaFutures {

  implicit val as = ActorSystem()
  implicit val am = ActorMaterializer()
  implicit val ec = as.dispatcher

  "API" should "return blockchaininfo" in {

    val client = new BitcoinRPCClient()

    val blockchainInfo = client.getBlockchainInfo().futureValue(Timeout(10 seconds))

    blockchainInfo shouldNot be(empty)

  }

  "API" should "return getnetworkinfo" in {

    val client = new BitcoinRPCClient()

    val response = client.getNetworkInfo().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

  "API" should "return getmempoolinfo" in {

    val client = new BitcoinRPCClient()

    val response = client.getMempoolInfo().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

  "API" should "return getpeerinfo" in {

    val client = new BitcoinRPCClient()

    val response = client.getPeerInfo().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

  "API" should "return getnettotals" in {

    val client = new BitcoinRPCClient()

    val response = client.getNetTotals().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

  "API" should "return getblockcount" in {

    val client = new BitcoinRPCClient()

    val response = client.getBlockCount().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

  "API" should "return getrawmempool" in {

    val client = new BitcoinRPCClient()

    val response = client.getRawMemPool().futureValue(Timeout(10 seconds))

    response shouldNot be(empty)

  }

}
