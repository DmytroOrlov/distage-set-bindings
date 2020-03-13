import sbtcrossproject.CrossPlugin.autoImport.crossProject
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import BuildHelper._

inThisBuild(
  List(
    name := "zio-macros",
    organization := "dev.zio",
    homepage := Some(url("https://github.com/zio/zio-macros")),
    developers := List(
      Developer(
        "mschuwalow",
        "Maxim Schuwalow",
        "maxim.schuwalow@gmail.com",
        url("https://github.com/mschuwalow")
      ),
      Developer(
        "ioleo",
        "Piotr Gołębiewski",
        "ioleo+zio@protonmail.com",
        url("https://github.com/ioleo")
      )
    ),
    scmInfo := Some(
      ScmInfo(
        homepage.value.get,
        "scm:git:git@github.com:zio/zio-macros.git"
      )
    ),
    licenses := Seq("Apache-2.0" -> url(s"${scmInfo.value.map(_.browseUrl).get}/blob/v${version.value}/LICENSE")),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc")
  )
)

ThisBuild / publishTo := Some("releases" at "https://nexus.com/nexus/content/repositories/releases")

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")
addCommandAlias("testJVM", ";coreExamplesJVM/compile;testExamplesJVM/compile;coreTestsJVM/test;testTestsJVM/test")
addCommandAlias("testJS", ";coreExamplesJS/compile;testExamplesJS/compile;coreTestsJS/test;testTestsJS/test")
addCommandAlias("testRelease", ";set every isSnapshot := false;+clean;+compile")

lazy val root = project
  .in(file("."))
  .settings(
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    core.jvm,
    core.js,
    coreExamples.jvm,
    coreExamples.js,
    coreTests.jvm,
    coreTests.js,
    test.jvm,
    test.js,
    testExamples.jvm,
    testExamples.js,
    testTests.jvm,
    testTests.js
  )
  .enablePlugins(ScalaJSPlugin)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .in(file("core"))
  .settings(stdSettings("zio-macros-core"))
  .settings(macroSettings)
  .settings(libraryDependencies += "dev.zio" %% "zio" % zioVersion)

lazy val coreExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("core-examples"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-core-examples"))
  .settings(examplesSettings)

lazy val coreTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("core-tests"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-core-tests"))
  .settings(testSettings)

lazy val test = crossProject(JSPlatform, JVMPlatform)
  .in(file("test"))
  .dependsOn(core)
  .settings(stdSettings("zio-macros-test"))
  .settings(macroSettings)
  .settings(libraryDependencies += "dev.zio" %% "zio-test" % zioVersion)

lazy val testExamples = crossProject(JSPlatform, JVMPlatform)
  .in(file("test-examples"))
  .dependsOn(test)
  .settings(stdSettings("zio-macros-test-examples"))
  .settings(examplesSettings)

lazy val testTests = crossProject(JSPlatform, JVMPlatform)
  .in(file("test-tests"))
  .dependsOn(test)
  .settings(stdSettings("zio-macros-test-tests"))
  .settings(testSettings)

val V = new {
  val zio = "1.0.0-RC17"
  val distage = "0.10.2-M8"

  val cats = "2.1.1"
  val pureConfig = "0.12.3"
  val akkaHttp = "10.1.11"
  val akka = "2.6.3"
  val monix = "3.1.0"
  val circe = "0.13.0"
  val circeExtras = "0.13.0"

  val betterMonadicFor = "0.3.1"
}

val Deps = new {
  val zio = "dev.zio" %% "zio" % V.zio
  val distageFramework = "io.7mind.izumi" %% "distage-framework" % V.distage
  val distageConfig = "io.7mind.izumi" %% "distage-extension-config" % V.distage
  val distageTestkitScalatest = "io.7mind.izumi" %% "distage-testkit-scalatest" % V.distage

  val betterMonadicFor = "com.olegpy" %% "better-monadic-for" % V.betterMonadicFor

  val cats = Seq("org.typelevel" %% "cats-core" % V.cats)

  val akka = List(
    "com.typesafe.akka" %% "akka-http" % V.akkaHttp,
    "com.typesafe.akka" %% "akka-slf4j" % V.akka,
    "com.typesafe.akka" %% "akka-stream" % V.akka,
  )

  val logging =
    Seq("ch.qos.logback" % "logback-classic" % "1.2.3", "net.logstash.logback" % "logstash-logback-encoder" % "6.3")

  val circe = List(
    "de.heikoseeberger" %% "akka-http-circe" % "1.31.0",
    "io.circe" %% "circe-core" % V.circe,
    "io.circe" %% "circe-generic" % V.circe,
    "io.circe" %% "circe-parser" % V.circe,
    "io.circe" %% "circe-generic-extras" % V.circeExtras
  )

  val monix = Seq(
    "io.monix" %% "monix-execution" % V.monix,
    "io.monix" %% "monix-eval" % V.monix
  )

  val pureconfig = Seq("com.github.pureconfig" %% "pureconfig" % V.pureConfig)

  val test = List(
    "org.scalatest" %% "scalatest" % "3.1.1" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % V.akkaHttp % Test,
    "com.typesafe.akka" %% "akka-stream-testkit" % V.akka % Test,
    distageTestkitScalatest % Test,
  )

  val mongoDriver = Seq(
    "org.mongodb.scala" %% "mongo-scala-driver" % "2.8.0",
    //Needed for SSL connection
    "io.netty" % "netty-all" % "4.1.47.Final"
  )
  val core = test ++ akka ++ logging ++ cats ++ monix ++ pureconfig ++ mongoDriver ++
    List(
      zio,
      distageFramework,
      distageConfig,
    )
}

lazy val `data-reader` = project
  .settings(
    libraryDependencies ++= Deps.core ++ Deps.test ++ Deps.circe,
  )
  .dependsOn(core.jvm)

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / libraryDependencies += compilerPlugin(Deps.betterMonadicFor)
ThisBuild / scalacOptions ++= List(
  "-Ymacro-annotations",
  "-language:higherKinds",
  "-language:reflectiveCalls",
  "-unchecked",
  "-deprecation",
  "-feature",
)
