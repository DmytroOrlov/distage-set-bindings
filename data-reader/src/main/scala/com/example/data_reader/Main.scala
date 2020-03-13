package com.example.data_reader

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteConcatenation._
import com.example.data_reader.api.Routing
import com.example.data_reader.api.application.PayloadToScoringApi
import com.example.data_reader.config.DataReaderConf
import com.example.data_reader.plugins.{DataReaderPlugin, ZIOPlugin}
import com.example.data_reader.utils.Validator
import com.mongodb.ConnectionString
import distage.{DIKey, GCMode, Id, Injector}
import izumi.distage.plugins.PluginConfig
import izumi.distage.plugins.load.PluginLoader
import izumi.distage.roles.{RoleAppLauncher, RoleAppMain}
import izumi.fundamentals.platform.cli.model.raw.RawRoleParams
import logstage.LogBIO
import monix.execution.Scheduler
import org.mongodb.scala.MongoClient.DEFAULT_CODEC_REGISTRY
import org.mongodb.scala.connection.NettyStreamFactoryFactory
import org.mongodb.scala.{MongoClient, MongoClientSettings, MongoDatabase}
import zio._
import zio.console._
import zio.macros.annotation.accessible

@accessible(">")
trait Env {
  def serviceName: UIO[String]

  def conf: UIO[DataReaderConf]

  def blockingScheduler: UIO[Scheduler]
}

object Env {
  private val service = "data-reader"

  def make(
      cfg: DataReaderConf,
      blockingSc: Scheduler@Id("blocking"),
  ) =
    new Env {
      val serviceName = IO.succeed(service)
      val conf = IO.succeed(cfg)
      val blockingScheduler = IO.succeed(blockingSc)
    }
}

@accessible(">")
trait Api {
  def payloadToScoringApi: UIO[PayloadToScoringApi]
}

object Api {
  val make = for {
    name <- Env.>.serviceName
    as <- ActorSys.>.system
    s <- ActorSys.>.scheduler
    cfg <- Env.>.conf
    impl <- IO(new PayloadToScoringApi)
  } yield new Api {
    val payloadToScoringApi = IO.succeed(impl)
  }
}

@accessible(">")
trait Routes {
  def route: UIO[Route]
}

object Routes {
  def make(routes: Set[Route]) =
    new Routes {
      val route = IO.succeed(routes.reduce(_ ~ _))
    }
}

@accessible(">")
trait HttpServer {
  def bind: UIO[Unit]
}

object HttpServer {
  val make = for {
    implicit0(as: ActorSystem) <- ActorSys.>.system.toManaged_
    cfg <- Env.>.conf.toManaged_
    // payloadToScoring <- Api.>.payloadToScoringApi.toManaged_
    routes <- Routes.>.route.toManaged_
    binding <- IO.fromFuture(_ => Http().bindAndHandle(routes /*~ payloadToScoring.route*/ , cfg.webServer.http.bindAddress, cfg.webServer.http.port))
      .toManaged(b => IO.fromFuture(_ => b.unbind()).ignore)
  } yield new HttpServer {
    val bind = IO.succeed(binding).unit
  }
}

@accessible(">")
trait ActorSys {
  def system: UIO[ActorSystem]

  def scheduler: UIO[Scheduler]
}

object ActorSys {
  val make = for {
    name <- Env.>.serviceName.toManaged_
    as <- UIO(ActorSystem(name)).toManaged(as => IO.fromFuture(_ => as.terminate()).ignore)
  } yield new ActorSys {
    val system = IO.succeed(as)
    val scheduler = IO.succeed(Scheduler(as.dispatcher))
  }
}

@accessible(">")
trait Mongo {
  def mongo: UIO[MongoDatabase]
}

object Mongo {
  def make(cfg: DataReaderConf) =
    for {
      connectionString <- Managed.succeed(new ConnectionString(cfg.uri.toString))
      client <- IO {
        MongoClient(
          MongoClientSettings
            .builder()
            .applyConnectionString(connectionString)
            .codecRegistry(DEFAULT_CODEC_REGISTRY)
            .streamFactoryFactory(NettyStreamFactoryFactory())
            .build(),
          None
        )
      }.toManaged(c => UIO(c.close()))
      impl = client.getDatabase(Option(connectionString.getDatabase).getOrElse("underwriting"))
    } yield new Mongo {
      val mongo = IO.succeed(impl)
    }
}

@accessible(">")
trait Log {
  def log: UIO[LogBIO[IO]]
}

object Log {
  def make(logBio: LogBIO[IO]) =
    new Log {
      val log = IO.succeed(logBio)
    }
}

object DataReaderMain extends RoleAppMain.Default(
  launcher = new RoleAppLauncher.LauncherBIO[IO] {
    val pluginConfig = PluginConfig.cached(
      packagesEnabled = Seq("com.example.data_reader.plugins"),
    )
  }
) {
  override val requiredRoles = Vector(RawRoleParams(DataReaderRole.id))

  val makeRoutes = for {
    name <- Env.>.serviceName
    cfg <- Env.>.conf
    actorSystem <- ActorSys.>.system
    sc <- ActorSys.>.scheduler
    blockingSc <- Env.>.blockingScheduler
    mongo <- Mongo.>.mongo
    routes = new Routing {}
  } yield routes.route
}
