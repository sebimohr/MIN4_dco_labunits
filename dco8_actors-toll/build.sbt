organization := "de.othr.dco"
name := "actors-toll"
version := "0.1.0"

scalaVersion := "2.13.11"

val akkaVersion = "2.6.17"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.6"
)
