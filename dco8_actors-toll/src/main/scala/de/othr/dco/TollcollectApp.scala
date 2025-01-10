package de.othr.dco

import akka.actor.typed.ActorSystem

object TollcollectApp extends App {

  val system = ActorSystem(TollSupervisor(), "toll-supervisor")

  system ! "hello-A3-1"
  system ! "hello-A9-1"
  system ! "fail-A9"
  system ! "hello-A3-1"
  system ! "hello-A9-1"
  system ! "sensor-A9-2-R AB 123"

}
