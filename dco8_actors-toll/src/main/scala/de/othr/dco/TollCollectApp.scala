package de.othr.dco

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SupervisorStrategy}
import de.othr.dco.SensorActor.{OnListing, SensorAction, SensorMessage}
import de.othr.dco.actor.DeadLetterReaderActor

import scala.language.postfixOps

object TollCollectApp extends App {

  val system = ActorSystem(TollSupervisor(), "toll-supervisor")

  system ! "hello-A3-1"
  system ! "hello-A9-1"
  system ! "fail-A9"
  system ! "hello-A3-1"
  system ! "hello-A9-1"
  system ! "sensor-A9-2-R AB 123"

  Thread.sleep(5000)
  system.terminate()
}

object TollSupervisor {
  def apply(): Behavior[String] = Behaviors.setup {
    context =>
      context.spawn(ChargingManager(), "charging-manager")
      context.spawn(DeadLetterReaderActor(), "dead-letter-reader")

      val regionManager = context.spawn(RegionManager(), "region-manager")

      Behaviors.receiveMessage {
        msg =>
          regionManager ! msg
          Behaviors.same
      }
  }
}

object RegionManager {
  def apply(): Behavior[String] = Behaviors.setup {
    context =>
      val highwayManagerA3 = context.spawn(supervise(HighwayManager("A3")), "highway-manager-A3")
      val highwayManagerA9 = context.spawn(supervise(HighwayManager("A9")), "highway-manager-A9")

      Behaviors.receiveMessagePartial[String] {
        case msg if msg.contains("A3") =>
          highwayManagerA3 ! msg
          Behaviors.same
        case msg =>
          highwayManagerA9 ! msg
          Behaviors.same
      }
  }

  private def supervise[T](behavior: Behavior[T]): Behavior[T] = Behaviors.supervise(behavior).onFailure(SupervisorStrategy.restart.withStopChildren(false))
}

object HighwayManager {
  def apply(highway: String): Behavior[String] = Behaviors.receive {
    case (_, message) if message.contains("fail") =>
      throw new IllegalStateException(s"Highway Error on highway $highway.")

    case (context, message) =>
      val msg_split = message.split("-")(2)
      val sensorName = s"$highway-$msg_split"

      // Spawn new sensor actor if it's not a context child yet.
      val sensor = context.child(sensorName) match {
        case Some(sensorActor) => sensorActor.unsafeUpcast[SensorAction]
        case None => context.spawn(SensorActor(sensorName), sensorName)
      }

      sensor ! SensorMessage(message)

      Behaviors.same
  }
}

object SensorActor {
  sealed trait SensorAction

  final case class SensorMessage(msg: String) extends SensorAction

  final case class OnListing(listing: Receptionist.Listing) extends SensorAction

  def apply(name: String): Behavior[SensorAction] = Behaviors.withStash[SensorAction](100) {
    stashBuffer => {
      Behaviors.setup[SensorAction] { context =>
        val adapter = context.messageAdapter((listing: Receptionist.Listing) => OnListing(listing))

        //noinspection FieldFromDelayedInit
        context.system.receptionist ! Receptionist.Subscribe(ChargingManager.ChargingManagerServiceKey, adapter)

        Behaviors.receiveMessagePartial {
          case OnListing(listing) =>
            println(s"Service reference now available for $name; unstashing stashed messages now: $listing")
            stashBuffer.unstashAll(
              //noinspection FieldFromDelayedInit
              ready(name, listing.serviceInstances(ChargingManager.ChargingManagerServiceKey))
            )
            Behaviors.same
          case other =>
            println(s"Stashing message for later: message = $other")
            stashBuffer.stash(other)
            Behaviors.same
        }
      }
    }
  }

  // "ready" state/behavior; after charging-manager ref(s) is/are available
  private def ready(sensorName: String, chargingActors: Set[ActorRef[ChargingManager.RouteUsageEvent]]): Behavior[SensorAction] =
    Behaviors.receivePartial[SensorAction] {

      case (ctx, SensorMessage(msg)) if msg.contains("sensor") =>
        // looping here, could be possible that more services with the same key were registered
        for (chargingActor <- chargingActors)
          chargingActor ! ChargingManager.RouteUsageEvent(msg)
        println(s"Message received by SensorActor $sensorName: msg=$msg context.self=${ctx.self}")
        Behaviors.same

      case (ctx, SensorMessage(msg)) =>
        println(s"Message received by SensorActor $sensorName: msg=$msg context.self=${ctx.self}")
        Behaviors.same

      // case OnListing(listing) => ... // if you want to get updates, in case a new service registered

    }.receiveSignal{
      case (context, signal) =>
        println(s"Signal received: name=$sensorName signal=$signal context.self=${context.self}")
        Behaviors.same
    }

}

object ChargingManager {

  val ChargingManagerServiceKey = ServiceKey[RouteUsageEvent]("charging-manager")

  final case class RouteUsageEvent(sensorReading: String)

  def apply(): Behavior[RouteUsageEvent] = Behaviors.setup {
    context =>
      context.system.receptionist ! Receptionist.Register(ChargingManagerServiceKey, context.self)
      println("Registered Charging Manager with Receptionist.")

      Behaviors.receive[RouteUsageEvent] {
        case (_, RouteUsageEvent(sensorReading)) =>
          print(sensorReading)
          Behaviors.same
      }
  }

}

object TruckActor {
  sealed trait TruckAction

  final case class TruckPaymentAction(plate: String, msg: String) extends TruckAction

  final case class TruckNotRegisteredAction(plate: String, msg: String) extends TruckAction
}

