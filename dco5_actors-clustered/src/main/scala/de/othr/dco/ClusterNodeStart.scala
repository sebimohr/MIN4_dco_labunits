package de.othr.dco

import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.actor.typed.{ActorSystem, SupervisorStrategy}
import com.typesafe.config.ConfigFactory
import de.othr.dco.Constants.ROUTER_POOL_SIZE
import de.othr.dco.LocalRouter.WorkerServiceKey
import de.othr.dco.WorkerActor.{PrintMessage, WorkFailure, WorkerAction}

import java.time.InstantSource.system

object Constants {
  val ROUTER_POOL_SIZE: Int = Runtime.getRuntime.availableProcessors() - 1
}

object ClusterNodeStart extends App {
  private val SystemName = "lab-cluster"

  private val config = ConfigFactory.load()
  private val port = config.getInt("akka.remote.artery.canonical.port")

  println(s"Starting up new node on port $port")

  if (port <= 2552) {
    ActorSystem(Behaviors.supervise[WorkerAction](LocalRouter()).onFailure(SupervisorStrategy.restart), SystemName)
  } else if (port == 2553) {
    val system: ActorSystem[WorkerAction] = ActorSystem(Behaviors.supervise[WorkerAction](Routers.group(WorkerServiceKey)).onFailure(SupervisorStrategy.restart), SystemName)

    Thread.sleep(3000)

    //  private val system: ActorSystem[WorkerAction] = ActorSystem(Routers.pool(ROUTER_POOL_SIZE)(WorkerActor()), "ClusterSystem", config)

    for (i <- 1 to ROUTER_POOL_SIZE) {
      system ! PrintMessage(s"Message $i")
    }

    system ! WorkFailure(s"Kill the cluster.")

    for (i <- ROUTER_POOL_SIZE + 1 to ROUTER_POOL_SIZE * 2) {
      system ! PrintMessage(s"Message $i")
    }
  }
}




