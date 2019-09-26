package com.nicolasmy

import java.time.{Instant, ZoneId, ZonedDateTime}

import scodec._
import scodec.bits._
import codecs._

case class BitcoinBlock(version: Long, hashPrevBlock: String, hashMerkleRoot: String, time: Long, bits: Long, nonce: Long) {

  def print() = {
    println(s"version: $version")
    println(s"hashPrevBlock: $hashPrevBlock")
    println(s"hashRoot: $hashMerkleRoot")
    println(s"Time ${ZonedDateTime.ofInstant(Instant.ofEpochSecond(time), ZoneId.of("UTC"))} | Bits $bits | Nonce $nonce")
    println(s"Max Target: ${maxTarget.toString(16)}")
  }

  def packedBitsToTarget(bits: Long) = BigInt(0xffffff & bits) << (8 * (((bits & 0xffffffff) >> 24) - 3)).toInt

  val maxTarget = packedBitsToTarget(bits)
}

object BitcoinBlock {


  val hash32 = scodec.codecs.bytes(32).exmap[String](Attempt.Successful(_).map(_.toHex),
    ByteVector.fromHex(_).map(Attempt.Successful(_)).getOrElse(Attempt.failure(Err("Invalid 256 bit hash"))))

  val headerCodec = uint32L :: hash32 :: hash32 :: uint32L :: uint32L :: uint32L

  def decode(input: BitVector) = (BitcoinBlock.apply _).tupled(headerCodec.decode(input).require.value.tupled)

}
