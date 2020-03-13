package com.example.data_reader.api

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

trait Routing {
  val route: Route = path("") {
    complete("root")
  }
}
