package de.othr.dco.actor.truck

import akka.actor.typed.{Behavior, scaladsl}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

import java.util.Date

object TruckActor {

  final case class Invoice(truckId: String, startedAt: String, exitedAt: String, km: Double, timestamp: Date)

  sealed trait TruckActorRequest
  case class RouteUsage(position: String, km: Double) extends TruckActorRequest
  case class RouteStart(position: String) extends TruckActorRequest
  case class RouteEnd(position: String, km: Double) extends TruckActorRequest

  def apply(truckId: String) : Behavior[TruckActorRequest] = {
    // TODO: Change into persistent behavior with snapshot(s), if applicable
    Behaviors.receiveMessage {
      msg =>
        println(s"Truck $truckId received: $msg")
        Behaviors.same
    }
  }

}
