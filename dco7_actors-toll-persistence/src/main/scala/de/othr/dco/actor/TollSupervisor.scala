package de.othr.dco.actor

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import de.othr.dco.actor.highway.RegionManagerActor
import de.othr.dco.actor.truck.ChargingManagerActor

object TollSupervisor {

  def apply() : Behavior[String] = Behaviors.setup{
    context => {
      context.spawn(DeadLetterReaderActor(), "deadletter-reader")
      context.spawn(ChargingManagerActor(), "charging-manager")

      val regionManager = context.spawn(RegionManagerActor("region-manager"), "region-manager")
      Behaviors.receiveMessage{
        msg => regionManager ! msg
        Behaviors.same
      }
    }
  }

}
