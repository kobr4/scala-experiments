lazy val akkaHttpVersion = "10.1.4"
lazy val akkaVersion    = "2.5.16"

lazy val root = (project in file(".")).enablePlugins(SbtWeb).enablePlugins(DockerPlugin).
  settings(
    inThisBuild(List(
      organization    := "com.nicolasmy",
      scalaVersion    := "2.12.6"
    )),
    (managedClasspath in Runtime) += (packageBin in Assets).value,
    WebKeys.packagePrefix in Assets := "public/",
    WebKeys.pipeline := WebKeys.pipeline.dependsOn(webpack.toTask("")).value,
    name := "btc-web",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "com.typesafe.play" %% "play-json" % "2.6.0",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-http" % "10.0.9",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
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
    repository = "btc-web",
    tag = Some("latest")))
}

test in assembly := {}