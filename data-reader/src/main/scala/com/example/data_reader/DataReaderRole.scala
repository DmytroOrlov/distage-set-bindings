package com.example.data_reader

import izumi.distage.roles.model.{RoleDescriptor, RoleTask}
import izumi.fundamentals.platform.cli.model.raw.RawEntrypointParams
import zio._

final class DataReaderRole(dataReaderApp: UIO[Unit]) extends RoleTask[Task] {
  def start(roleParameters: RawEntrypointParams, freeArgs: Vector[String]) =
    dataReaderApp
}

object DataReaderRole extends RoleDescriptor {
  val id = "data-reader"

  val makeApp = HttpServer.>.bind *> IO.never
}
