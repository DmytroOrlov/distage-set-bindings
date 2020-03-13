package com.example.data_reader.plugins

import akka.http.scaladsl.server.Route
import com.example.data_reader._
import com.example.data_reader.config.DataReaderConf
import com.example.data_reader.utils.Validator
import distage.config.ConfigModuleDef
import distage.plugins.PluginDef
import distage.{ProviderMagnet, Tag}
import izumi.distage.constructors.TraitConstructor
import monix.execution.Scheduler
import zio.UIO

object DataReaderPlugin extends PluginDef with ConfigModuleDef {
  def provideCake[R: TraitConstructor, A: Tag](fn: R => A): ProviderMagnet[A] =
    TraitConstructor[R].provider.map(fn)

  makeConfig[DataReaderConf]("data-reader")

  make[Scheduler].named("blocking").fromValue(Scheduler.io(name = "blocking"))
  make[Env].from(Env.make _)
  make[Api].fromEffect(provideCake(Api.make.provide))
  make[ActorSys].fromResource(provideCake(ActorSys.make.provide))
  make[Mongo].fromResource(Mongo.make _)
  make[HttpServer].fromResource(provideCake(HttpServer.make.provide))
  make[Routes].from(Routes.make _)
  many[Route]
    .addEffect(provideCake(Api.>.payloadToScoringApi.map(_.route).provide))
    .addEffect(provideCake(DataReaderMain.makeRoutes.provide))
  make[UIO[Unit]].from(provideCake(DataReaderRole.makeApp.provide))
  make[DataReaderRole]
}
