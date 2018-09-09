lazy val akkaHttpVersion = "10.1.4"
lazy val akkaVersion    = "2.5.16"

lazy val root = (project in file(".")).enablePlugins(SbtWeb).
  settings(
    inThisBuild(List(
      organization    := "com.nicolasmy",
      scalaVersion    := "2.12.6"
    )),
    (managedClasspath in Runtime) += (packageBin in Assets).value,
    WebKeys.packagePrefix in Assets := "public/",
    name := "btc-web",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-xml"        % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"          % akkaVersion,

      "com.typesafe.play" %% "play-json" % "2.6.0",

      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
      "com.typesafe.akka" %% "akka-http" % "10.0.9",

      "com.typesafe.akka" %% "akka-http-testkit"    % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-testkit"         % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"  % akkaVersion     % Test,
      "org.scalatest"     %% "scalatest"            % "3.0.5"         % Test
    )
  )


JsEngineKeys.engineType := JsEngineKeys.EngineType.Node

BabelKeys.options := WebJs.JS.Object(
  "presets" -> List("react")
  //"presets" -> List("stage-0")
  // More options ...
)
