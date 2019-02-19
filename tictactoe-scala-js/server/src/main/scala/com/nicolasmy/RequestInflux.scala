package com.nicolasmy

import java.net.InetAddress

import akka.http.scaladsl.server.{Directive0, Route}
import com.paulgoldbaum.influxdbclient.{InfluxDB, Point}

import scala.concurrent.ExecutionContext

object RequestInflux extends Directive0 {

  implicit val ec = ExecutionContext.global

  val localhost: InetAddress = InetAddress.getLocalHost
  val localIpAddress: String = localhost.getHostAddress

  val database = InfluxDB.connect(DefaultConfiguration.Influx.Host, DefaultConfiguration.Influx.Port)
    .selectDatabase(DefaultConfiguration.Influx.DB)

  override def tapply(f: Unit => Route): Route = {
    ctx =>
      val point = Point("requests")
        .addTag("host", localIpAddress)
        .addField("path", ctx.request.uri.path.toString())
      database.write(point)
      f()(ctx)
  }
}
