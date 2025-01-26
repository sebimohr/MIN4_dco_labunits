package de.othr.dco.actor.truck

import akka.actor.typed.Behavior
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.{Effect, EventSourcedBehavior}

import java.util.Date
import scala.annotation.unused


object TruckActor {
  sealed trait TruckActorRequest
  case class RouteUsage(position: String, km: Double) extends TruckActorRequest
  case class RouteStart(position: String) extends TruckActorRequest
  case class RouteEnd(position: String, km: Double) extends TruckActorRequest

  private sealed trait TruckActorEvent
  private sealed case class TruckEntering(position: String, timeEntered: Date) extends TruckActorEvent
  private sealed case class TruckDriving(position: String, km: Double) extends TruckActorEvent
  private sealed case class TruckLeaving(position: String, km: Double) extends TruckActorEvent

  private final case class MovingState(kmDriven: Double, startPosition: String, startTime: Date)
  private final case class TruckActorState(truckId: String, invoiced: List[Invoice], movingState: Option[MovingState])
  private final case class Invoice(truckId: String, startedAt: String, exitedAt: String, km: Double, timestamp: Date)

  def apply(truckId: String): Behavior[TruckActorRequest] = {
    EventSourcedBehavior[TruckActorRequest, TruckActorEvent, TruckActorState](
      persistenceId = PersistenceId.ofUniqueId(truckId),
      emptyState = TruckActorState(truckId, List.empty, None),
      commandHandler = truckRequestHandler,
      eventHandler = truckEventHandler
      )
  }

  private def truckRequestHandler(@unused state: TruckActorState,
                                  command: TruckActorRequest): Effect[TruckActorEvent, TruckActorState] = {
    command match {
      case RouteStart(position) =>
        println(s"-------------- Request received for ${state.truckId}, RouteStart on position $position")
        Effect.persist(TruckEntering(position, new Date()))
      case RouteUsage(position, km) =>
        println(s"-------------- Request received for ${state.truckId}, RouteUsage on position $position and km $km")
        Effect.persist(TruckDriving(position, km))
      case RouteEnd(position, km) =>
        println(s"-------------- Request received for ${state.truckId}, RouteEnd on position $position and km $km")
        Effect.persist(TruckLeaving(position, km))
    }
  }

  private def truckEventHandler(state: TruckActorState, event: TruckActorEvent): TruckActorState = {
    event match {
      case TruckEntering(position, timeEntered) =>
        println(s"-------------- Truck entered on position $position on time $timeEntered.")
        state.copy(movingState = Some(MovingState(0, position, timeEntered)))
      case TruckDriving(position, km) =>
        println(s"-------------- Truck is driving on position $position with ${
          state
            .movingState
            .get
            .kmDriven + km
        } km driven.")
        state.copy(movingState = state.movingState match {
          case Some(MovingState(kmDriven, start, time)) => Some(MovingState(kmDriven + km, start, time))
          case None => throw new IllegalStateException("TruckDrivingEvent, but truck didn't start a route.")
        })
      case TruckLeaving(position, km) =>
        println(s"-------------- Truck is leaving on position $position with ${
          state
            .movingState
            .get
            .kmDriven + km
        } km driven.")
        val invoice = state.movingState match {
          case Some(MovingState(kmDriven, start, time)) =>
            Invoice(state.truckId, start, position, kmDriven + km, time)
          case None => throw new IllegalStateException("TruckLeavingEvent, but truck didn't start a route.")
        }
        state.copy(invoiced = state.invoiced :+ invoice)
    }
  }
}
