package de.othr.dco

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import java.util.UUID

object WorkerActor extends App {
  sealed trait WorkerAction

  final case class PrintMessage(msg: String) extends WorkerAction

  final case class WorkFailure(msg: String) extends WorkerAction

  def apply(): Behavior[WorkerAction] = Behaviors.setup[WorkerAction] {
    _ =>
      val ownName = UUID.randomUUID().toString.substring(0, 8).toUpperCase()

      Behaviors.receiveMessagePartial[WorkerAction] {
        case PrintMessage(msg) =>
          println(s"Actor $ownName received $msg")
          Behaviors.same
        case WorkFailure(msg) =>
          println(s"Actor $ownName simulates failure with message $msg")
          throw new IllegalStateException(s"Actor $ownName failed with $msg")
      }
  }
}
