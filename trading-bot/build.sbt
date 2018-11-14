libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"
libraryDependencies += "com.enragedginger" %% "akka-quartz-scheduler" % "1.7.0-akka-2.5.x"
libraryDependencies += "com.github.cb372" %% "scalacache-guava" % "0.24.3"
//libraryDependencies += "com.pauldijou" %% "jwt-play-json" % "0.19.0"
libraryDependencies += "com.github.daddykotex" %% "courier" % "1.0.0-RC1"
libraryDependencies += "com.typesafe.slick" %% "slick" % "3.2.3"
libraryDependencies += "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3"
libraryDependencies += "mysql" % "mysql-connector-java" % "8.0.11"
libraryDependencies += "com.lihaoyi" %% "scalatags" % "0.6.7"
libraryDependencies += "com.sun.mail" % "javax.mail" % "1.6.2"
//libraryDependencies += "org.bouncycastle" % "bcprov-ext-jdk16" % "1.46"
libraryDependencies += ("com.pauldijou" %% "jwt-play-json" % "0.19.0")
  .exclude("org.bouncycastle", "bcpkix-jdk15on")
  .exclude("org.bouncycastle", "bcprov-jdk15on")
libraryDependencies +=  "org.bouncycastle"  %  "bcpkix-jdk15on" % "1.60" % Provided
libraryDependencies += "com.github.t3hnar" %% "scala-bcrypt" % "3.1"
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
    add(file("lib/bcpkix-jdk15on-1.60.jar"),"/docker-java-home/jre/lib/ext/")
    add(file("lib/bcprov-jdk15on-1.60.jar"),"/docker-java-home/jre/lib/ext/")
    runRaw("/bin/echo 'security.provider.10=org.bouncycastle.jce.provider.BouncyCastleProvider' >> /docker-java-home/jre/lib/security/java.security")
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

assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case "javamail.default.address.map" :: Nil => MergeStrategy.first
      case "mailcap.default" :: Nil => MergeStrategy.first
      case "mimetypes.default" :: Nil => MergeStrategy.first
      case "mailcap" :: Nil => MergeStrategy.first
      case "services" :: "java.security.provider" :: Nil => MergeStrategy.first
      case _ => MergeStrategy.discard
    }

  case x => MergeStrategy.first
}