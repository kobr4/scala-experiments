libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.enragedginger" %% "akka-quartz-scheduler" % "1.7.0-akka-2.5.x"
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.24.3"
libraryDependencies += "com.pauldijou" %% "jwt-play-json" % "0.19.0"
libraryDependencies += "com.github.daddykotex" %% "courier" % "1.0.0-RC1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.16.0" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % Test

Test / testOptions := Seq(Tests.Filter(s => s.endsWith("Test")))

lazy val root = (project in file(".")).enablePlugins(SbtWeb).enablePlugins(DockerPlugin).
  settings(
    inThisBuild(List(
      organization    := "com.nicolasmy",
      scalaVersion    := "2.12.6"
    )),
    (managedClasspath in Runtime) += (packageBin in Assets).value,
    WebKeys.packagePrefix in Assets := "public/",
    WebKeys.pipeline := WebKeys.pipeline.dependsOn(webpack.toTask("")).value,
    name := "trading-bot",(managedClasspath in Runtime) += (packageBin in Assets).value,
  )

JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

dockerfile in docker := {
  // The assembly task generates a fat JAR file
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:8-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

imageNames in docker := {
  Seq(ImageName(
    registry = Some("10.8.0.1:5000"),
    repository = "trading-bot",
    tag = Some("latest")))
}

Compile/mainClass := Some("com.kobr4.tradebot.QuickstartServer")