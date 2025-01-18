package de.othr.dco.webshop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import de.othr.dco.webshop.Entities.{Order, Payment}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

object PaymentActor extends App {
  sealed trait PaymentAction

  /**
   * Collect the payment for a given order.
   *
   * @param order   The order that should be paid.
   * @param replyTo The requesting actor.
   */
  case class CollectPayment(order: Order, replyTo: ActorRef[WebshopActor.WebshopAction]) extends PaymentAction

  def apply(): Behavior[PaymentAction] = processPayment()

  private def processPayment(paymentList: List[Payment] = List.empty): Behavior[PaymentAction] = Behaviors.receive[PaymentAction] {
    (context, message) => {
      message match {
        case CollectPayment(order, replyTo) =>
          implicit val executionContext: ExecutionContextExecutor = context.executionContext

          val paymentFuture: Future[List[Payment]] = Future {
            Thread.sleep(500 * order.user.id)
            var priceToPay = 0.0
            order.items.map(_.price).foreach(priceToPay += _)
            paymentList :+ Payment(order, BigDecimal(priceToPay).setScale(2, BigDecimal.RoundingMode.HALF_DOWN).toDouble)
          }


          paymentFuture.onComplete {
            case Success(paymentList) =>
              // Update list and respond to the requesting actor.
              processPayment(paymentList)
              replyTo ! WebshopActor.PaymentCollected(paymentList.last)
            case Failure(exception) =>
              replyTo ! WebshopActor.PaymentFailure(order, exception)
          }

          Behaviors.same
      }
    }
  }
}
