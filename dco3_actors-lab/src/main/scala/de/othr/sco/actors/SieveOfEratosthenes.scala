package de.othr.sco.actors

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import de.othr.sco.actors.CountPrimesActor.FinishExecution

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}


object SieveOfEratosthenes extends App {
  def apply(startRange: List[Int]): Behavior[Any] = Behaviors.setup {
    context => {
      val counterActorRef = context.spawn(CountPrimesActor(), "counter-actor")

      val firstSieveActor = context.spawn(SievePrimesActor(counterActorRef), "sieve-actor-initial")
      firstSieveActor ! SievePrimesActor.DetermineNextPrime(startRange)

      Behaviors.ignore
    }
  }

  private val numberRange = 1 to 100

  // Setup and start your Actor System here
  ActorSystem(SieveOfEratosthenes(numberRange.toList), "eratosthenes-system")
}

object SievePrimesActor extends App {
  case class DetermineNextPrime(intList: List[Int])

  def apply(counterActor: ActorRef[CountPrimesActor.CounterCommand]): Behavior[DetermineNextPrime] = Behaviors.receive {
    (context, message) => {
      message match {
        case DetermineNextPrime(intList) if intList.isEmpty =>
          // Finish the actorSystem.
          counterActor ! FinishExecution
        case DetermineNextPrime(firstListItem :: restOfList) =>
          // Report new prime to counter.
          counterActor ! CountPrimesActor.IncrementCounter(firstListItem)
          val remainingRange =
            if (firstListItem == 1) {
              // 1 is the default, so there shouldn't be any elements removed from the List.
              restOfList
            } else {
              // Filter out each number that's not prime to the current firstListItem of the rest of the List.
              restOfList.filter(_ % firstListItem != 0)
            }

          // Recursively call the SieveActor with then new remaining numbers.
          val nextPipelinedActor = context.spawn(SievePrimesActor(counterActor), s"sieve-actor-after-$firstListItem")
          nextPipelinedActor ! DetermineNextPrime(remainingRange)
      }
    }
      Behaviors.same
  }
}

object CountPrimesActor extends App {
  sealed trait CounterCommand

  case class IncrementCounter(primeNumber: Int) extends CounterCommand
  case object FinishExecution extends CounterCommand

  // Initializing Counter with 0 at the beginning.
  def apply(): Behavior[CounterCommand] = setupNewBehavior(0)

  // This method is only here to count the number of primes reported from SieveActor.
  private def setupNewBehavior(counter: Int, primeList: List[Int] = List.empty): Behavior[CounterCommand] = Behaviors.receive {
    (context, message) => {
      // As long as there is no new message, nothing is done.
      message match {
        case IncrementCounter(newPrime) =>
          // Recursively call setupNewBehavior with new counter value and the prime appended to the List.
          setupNewBehavior(counter + 1, primeList :+ newPrime)
        case FinishExecution =>
          println(s"FINISHED   Number of prime numbers: $counter")
          primeList.foreach(i => print(s"$i, "))
          println()

          // Given as implicit argument to future.
          implicit val system: ExecutionContextExecutor = context.system.executionContext

          // Future gets executed as soon as context.system.executionContext is terminated.
          val future = context.system.whenTerminated
          future onComplete {
            case Success(_) => println("Actor system terminated successfully!")
            case Failure(ex) => println(s"Actor system termination failed: ${ex.getMessage}")
          }

          context.system.terminate()

          // Report stopped behavior back to calling Actor.
          Behaviors.stopped
      }
    }
  }
}
