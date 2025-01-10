package de.othr.dco

import com.typesafe.config.ConfigFactory

object ClusterNodeStart extends App {

  val config = ConfigFactory.load()
  val port = config.getInt("akka.remote.artery.canonical.port")

  println(s"Starting up new node on port $port")


}
