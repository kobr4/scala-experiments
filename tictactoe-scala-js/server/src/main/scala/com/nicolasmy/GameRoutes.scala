package com.nicolasmy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait GameRoutes {

  lazy val gameRoutes: Route =
    pathPrefix("public") {
      getFromResourceDirectory("public")
    } ~ pathSingleSlash {
      getFromResource("public/index.html")
    }

}
