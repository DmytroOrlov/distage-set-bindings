package com.example.data_reader.api.application

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

class PayloadToScoringApi {
  val route: Route = pathPrefix("1") {
    complete("PayloadToScoringApi")
  }
}
