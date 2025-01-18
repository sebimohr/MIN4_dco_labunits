package de.othr.dco.webshop

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import de.othr.dco.webshop.Entities.{Item, Order, Payment, User}

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}
import scala.language.postfixOps

object WebshopActor extends App {
  sealed trait WebshopAction

  /**
   * Adds items to the basket of the given user.
   *
   * @param user The user.
   * @param item The new item, that should be added to their basket.
   */
  case class AddItemToBasket(user: User, item: Item) extends WebshopAction

  /**
   * Makes an order by retrieving all items in the basket and processing the payment.
   *
   * @param user The user, that requested the order.
   */
  case class MakeOrder(user: User) extends WebshopAction

  sealed trait WebshopResponse extends WebshopAction

  /**
   * Response from the BasketActor with all the items that are in the basket of the given user.
   *
   * @param user     The user.
   * @param itemList A List of all items that are currently in the user's basket.
   */
  case class AllItemsForUser(user: User, itemList: List[Item]) extends WebshopResponse

  /**
   * Response from the PaymentActor when the payment has been collected successfully.
   *
   * @param payment The payment for an order.
   */
  case class PaymentCollected(payment: Payment) extends WebshopResponse

  /**
   * Response from the PaymentActor when the payment couldn't be collected.
   *
   * @param order     The order, that couldn't be completed.
   * @param exception The exception that has been thrown.
   */
  case class PaymentFailure(order: Order, exception: Throwable) extends WebshopResponse

  def apply(/*paymentActorRef: ActorRef[PaymentActor.PaymentAction], basketActorRef: ActorRef[BasketActor.BasketAction]*/): Behavior[WebshopAction] = Behaviors.setup {
    context => {
      val paymentActorRef = context.spawn(PaymentActor(), "payment-actor")
      val basketActorRef = context.spawn(BasketActor(), "basket-actor")

      implicit val timeout: Timeout = 5 seconds

      Behaviors.receiveMessage {
        case AddItemToBasket(user, item) =>
          context.log.debug(s"Adding item ${item.name} to basket of user ${user.name}.")
          // Only send to the actor, don't wait for response.
          basketActorRef ! BasketActor.AddItemToUserBasket(user, item)
          Behaviors.same
        case MakeOrder(user) =>
          context.log.debug(s"Make order for user ${user.name}.")
          // Wait for response and handle it.
          context.ask(basketActorRef, ref => BasketActor.GetAllItemsForUser(user, ref)) {
            case Success(itemsForUser) =>
              val res = itemsForUser.asInstanceOf[AllItemsForUser]
              AllItemsForUser(res.user, res.itemList)
            case Failure(_) =>
              AllItemsForUser(user, List.empty[Item])
          }
          Behaviors.same
        case AllItemsForUser(user, itemList) =>
          // Wait for response and handle it.
          context.ask(paymentActorRef, ref => PaymentActor.CollectPayment(Order(user, itemList), ref)) {
            case Success(orderDetails) =>
              PaymentCollected(orderDetails.asInstanceOf[PaymentCollected].payment)
            case Failure(exception) =>
              PaymentFailure(Order(user, List.empty[Item]), exception)
          }
          Behaviors.same
        case PaymentCollected(payment) =>
          context.log.info(s"Order completed. Price: ${payment.pricePaid}")
          Behaviors.same
        case PaymentFailure(order, exception) =>
          context.log.error(s"Payment failed for order of user ${order.user.name}: ${exception.getMessage}")
          Behaviors.same
      }
    }
  }
}
