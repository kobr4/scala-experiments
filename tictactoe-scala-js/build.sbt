import sbtdocker.DockerPlugin.autoImport.imageNames

lazy val akkaHttpVersion = "10.1.4"
lazy val akkaVersion = "2.5.16"

lazy val server = project
  .in(file("server"))
  .enablePlugins(SbtWeb)
  .enablePlugins(DockerPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.nicolasmy",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.12.6"
    )),
    name := "tictactoe-scala-js",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "org.scalatest" %%% "scalatest" % "3.0.5" % "test"
    ),
    (managedClasspath in Runtime) += (packageBin in Assets).value,
    WebKeys.packagePrefix in Assets := "public/",
    WebKeys.pipeline := WebKeys.pipeline.dependsOn(webpack.toTask("")).value,
    
    dockerfile in docker := {
      // The assembly task generates a fat JAR file
      val artifact: File = assembly.value
      val artifactTargetPath = s"/app/${artifact.name}"

      new Dockerfile {
        from("openjdk:8-jre")
        entryPoint("java", "-jar", artifactTargetPath)
      }
    },

    imageNames in docker := {
      Seq(ImageName(
        registry = Some("10.8.0.1:5000"),
        repository = "tictactoe",
        tag = Some("latest")))
    }

  )




lazy val root = project
  .in(file("."))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    inThisBuild(List(
      organization := "com.nicolasmy",
      version := "0.1-SNAPSHOT",
      scalaVersion := "2.12.6"
    )),
    name := "tictactoe-scala-js",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "0.9.5",
      "org.scalatest" %%% "scalatest" % "3.0.5" % "test",
      "com.github.chandu0101" %%% "sri-web" % "0.7.1"
    ),
    scalaJSUseMainModuleInitializer := true
  )
JsEngineKeys.engineType := JsEngineKeys.EngineType.Node
scalaJSModuleKind := ModuleKind.CommonJSModule


val SJS_OUTPUT_PATH = "assets/scalajs-output.js"
val fastOptWeb = Def.taskKey[Unit]("Generate web output file for fastOptJS")
artifactPath in Compile in fastOptJS :=
  server.base / SJS_OUTPUT_PATH
//fastOptWeb in Compile := {
//val launcher = (scalaJSLauncher in Compile).value.data.content
//IO.write(baseDirectory.value / "assets/scalajs-output-launcher.js", launcher)
//}


// Automatically generate index-dev.html which uses *-fastopt.js
/*
resourceGenerators in Compile += Def.task {
  val source = (resourceDirectory in Compile).value / "index.html"
  val target = (resourceManaged in Compile).value / "index-dev.html"

  val fullFileName = (artifactPath in (Compile, fullOptJS)).value.getName
  val fastFileName = (artifactPath in (Compile, fastOptJS)).value.getName

  IO.writeLines(target,
    IO.readLines(source).map {
      line => line.replace(fullFileName, fastFileName)
    }
  )

  Seq(target)
}.taskValue
*/

//Compile/mainClass := Some("com.nicolasmy.QuickStartServer")
//Compile/mainClass := Some("MainApp")
/*
assemblyMergeStrategy in assembly := {
  case s if s.contains("_sjs") => MergeStrategy.discard
}
*/
/*
assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp.filter(f => (f.data.getName contains "_sjs") || (f.data.getName contains "scalajs")  )
}
*/