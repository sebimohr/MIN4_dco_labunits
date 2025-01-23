package de.othr.dco

import akka.actor.typed.Behavior
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import de.othr.dco.Constants.ROUTER_POOL_SIZE
import de.othr.dco.WorkerActor.WorkerAction

object LocalRouter {
  val WorkerServiceKey: ServiceKey[WorkerAction] = ServiceKey[WorkerAction]("worker")

  def apply(): Behavior[WorkerAction] = Behaviors.setup[WorkerAction] {
    ctx =>
      ctx.system.receptionist ! Receptionist.Register(WorkerServiceKey, ctx.self)

      println(s"Creating LocalRouter -> Routers.pool($ROUTER_POOL_SIZE)")

      Routers.pool(ROUTER_POOL_SIZE)(WorkerActor())
  }
}
