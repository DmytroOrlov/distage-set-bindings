package example

import distage.{Tag, ProviderMagnet, ModuleDef, Injector, GCMode, DIKey}
import izumi.distage.constructors.TraitConstructor
import zio._
import zio.console.{Console, putStrLn}
import zio.macros.annotation.accessible

@accessible(">")
trait Hello {
//  val hello: Hello.Service[Any]
  def hello: UIO[String]
}

object Hello {
/*
  trait Service[R] {
    def hello: URIO[R, String]
  }
*/
}

@accessible(">")
trait World {
//  val world: World.Service[Any]
  def world: UIO[String]
}

/*
object World {
  trait Service[R] {
    def world: URIO[R, String]
  }
}
*/

object Main extends App {

  // Environment forwarders that allow
  // using service functions from everywhere

//  val hello: URIO[ {val hello: Hello.Service}, String] = ZIO.accessM(_.hello.hello)
  //  val world: URIO[World, String] = ZIO.accessM(_.world.world)

  // service implementations

  val makeHello = {
    (for {
      _ <- putStrLn("Creating Enterprise Hellower...")
      hello = new Hello/*.Service[Any]*/ {
        val hello = UIO("Hello")
      }
    } yield hello).toManaged { _ =>
      putStrLn("Shutting down Enterprise Hellower")
    }
  }

  val makeWorld = {
    for {
      counter <- Ref.make(0)
    } yield new World/*.Service[Any]*/ {
      val world = counter.get.map(c => if (c < 1) "World" else "THE World")
    }
  }

  // the main function

  val turboFunctionalHelloWorld = {
    for {
      hello <- Hello.>.hello
      world <- World.>.world
      _ <- putStrLn(s"$hello $world")
    } yield ()
  }

  // a generic function that creates an `R` trait where all fields are populated from the object graph

  def provideCake[R: TraitConstructor, A: Tag](fn: R => A): ProviderMagnet[A] = {
    TraitConstructor[R].provider.map(fn)
  }

  val definition = new ModuleDef {
    make[Hello/*.Service[Any]*/].fromResource(provideCake(makeHello.provide(_)))
    make[World/*.Service[Any]*/].fromEffect(makeWorld)
    make[Console.Service[Any]].fromValue(Console.Live.console)
    make[UIO[Unit]].from(provideCake(turboFunctionalHelloWorld.provide))
  }

  val main = Injector()
    .produceF[Task](definition, GCMode(DIKey.get[UIO[Unit]]))
    .use(_.get[UIO[Unit]])

  def run(args: List[String]) =
    main
      .foldM(
        e => putStrLn(e.getMessage).as(1),
        _ => IO.succeed(0)
      )
}
