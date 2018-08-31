scalaVersion := "2.12.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"
libraryDependencies += "com.typesafe" % "config" % "1.3.2"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.16.0" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % Test

Test / testOptions := Seq(Tests.Filter(s => s.endsWith("Test")))