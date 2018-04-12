scalaVersion := "2.12.3"

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.6.0"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test
libraryDependencies += "com.github.tomakehurst" % "wiremock" % "2.16.0" % Test
libraryDependencies += "org.mockito" % "mockito-all" % "1.9.5" % Test