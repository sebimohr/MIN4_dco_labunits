organization := "de.othr.dco"
name := "actors-toll-persistence"
version := "0.1.0"

scalaVersion := "2.13.7"

val akkaVersion = "2.6.17"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "ch.qos.logback" % "logback-classic" % "1.2.6"
)