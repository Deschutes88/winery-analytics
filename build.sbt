name := "swines"

version := "1.01"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  //  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7",
  "com.lightbend.akka" %% "akka-stream-alpakka-file" % "1.0-M3",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
//
val circeVersion = "0.10.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
// https://mvnrepository.com/artifact/io.circe/circe-derivation
libraryDependencies += "io.circe" %% "circe-derivation" % "0.11.0-M1"
//// https://mvnrepository.com/artifact/io.circe/circe-derivation
//libraryDependencies += "io.circe" %% "circe-derivation" % "0.10.0-M1"
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)

libraryDependencies += "com.fasterxml.jackson.core" % "jackson-core" % "2.7.9"
libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.7.9"

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"
libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

// https://mvnrepository.com/artifact/com.typesafe/config


mainClass := Some("swines.ReadReviews")
enablePlugins(JavaAppPackaging)
