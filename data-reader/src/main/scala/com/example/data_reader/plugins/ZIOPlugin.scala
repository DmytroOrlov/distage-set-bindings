package com.example.data_reader.plugins

import com.example.data_reader.Log
import distage.config.ConfigModuleDef
import distage.plugins.PluginDef
import izumi.distage.effect.modules.ZIODIEffectModule
import logstage.LogBIO
import zio.IO
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console

object ZIOPlugin extends ZIODIEffectModule with PluginDef {
  make[Console.Service[Any]].fromValue(Console.Live.console)
  make[Blocking.Service[Any]].fromValue(Blocking.Live.blocking)
  make[Clock.Service[Any]].fromValue(Clock.Live.clock)
  make[LogBIO[IO]].from(LogBIO.fromLogger[IO] _)
  make[Log].from(Log.make _)
}
