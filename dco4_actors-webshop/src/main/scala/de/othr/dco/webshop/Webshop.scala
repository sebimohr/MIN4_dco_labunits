package de.othr.dco.webshop

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.util.Timeout
import de.othr.dco.webshop.Entities.{Item, Order, Payment, User}
import de.othr.dco.webshop.WebshopActor.{AddItemToBasket, AllItemsForUser, MakeOrder}

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

object Entities {
  case class Item(name: String, price: Double)

  case class User(id: Int, name: String)

  case class Order(user: User, items: List[Item])

  case class Payment(order: Order, pricePaid: Double)
}

object Webshop extends App {
  //noinspection DuplicatedCode
  def apply(): Behavior[Any] = Behaviors.setup {
    context => {
      context.log.info("Starting up webshop.")

      val webshopActor = context.spawn(WebshopActor(/*paymentActorRef, basketActorRef*/), "webshop-actor")

      val user1 = User(1, "Alice")
      val user2 = User(2, "Max")

      val item1 = Item("Milk", 1.20)
      val item2 = Item("Butter", 3.00)
      val item3 = Item("Eggs", 3.20)
      val item4 = Item("Flour", 0.80)

      webshopActor ! AddItemToBasket(user1, item1)
      webshopActor ! AddItemToBasket(user1, item1)
      webshopActor ! AddItemToBasket(user1, item2)
      webshopActor ! AddItemToBasket(user1, item3)

      webshopActor ! AddItemToBasket(user2, item1)
      webshopActor ! AddItemToBasket(user2, item2)
      webshopActor ! AddItemToBasket(user2, item3)
      webshopActor ! AddItemToBasket(user2, item4)

      webshopActor ! MakeOrder(user1)
      webshopActor ! MakeOrder(user2)

      Thread.sleep(5000)
      context.log.debug("Terminating webshop.")
      context.system.terminate()
      Behaviors.stopped
    }
  }

  ActorSystem(Webshop(), "webshop-system")
}

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
   * @param user The user.
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
   * @param order The order, that couldn't be completed.
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

object PaymentActor extends App {
  sealed trait PaymentAction

  /**
   * Collect the payment for a given order.
   *
   * @param order The order that should be paid.
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

object BasketActor extends App {
  sealed trait BasketAction

  /**
   * Adds an item to the given user's basket.
   *
   * @param user The user.
   * @param item The item, that should be added to the user's basket.
   */
  case class AddItemToUserBasket(user: User, item: Item) extends BasketAction

  /**
   * Gets all items for the given user's basket.
   *
   * @param user The user.
   * @param replyTo The requesting actor.
   */
  case class GetAllItemsForUser(user: User, replyTo: ActorRef[WebshopActor.WebshopAction]) extends BasketAction

  def apply(): Behavior[BasketAction] = handleBasket()

  private def handleBasket(basketList: List[Order] = List.empty): Behavior[BasketAction] = Behaviors.receive {
    (context, message) => {
      message match {
        case AddItemToUserBasket(user, item) =>
          // Update item where userId matches with userId from the message.
          var basketUpdated = if(!basketList.exists(_.user.id == user.id)) {
            basketList :+ Order(user, List.empty)
          } else basketList

          basketUpdated = basketUpdated.map {
            case basketItem if basketItem.user.id == user.id => basketItem.copy(items = basketItem.items :+ item)
            case basketItem => basketItem
          }
          context.log.info(s"Item ${item.name} added for user ${user.name}.")
          context.log.debug(s"Basket updated. Orders in basket: ${basketUpdated.size}")

          handleBasket(basketUpdated)
        case GetAllItemsForUser(user, replyTo) =>
          // Gets all items for a specific user, returns that users itemList in basket
          context.log.info(s"Retrieving all items for user ${user.name}")
          val userBasketItemList: List[Item] = basketList.find(_.user.id == user.id).map(_.items).getOrElse(List.empty)
          context.log.info(s"Found ${userBasketItemList.size} items.")

          // Reply to the requesting actor with the found items.
          replyTo ! AllItemsForUser(user, userBasketItemList)
          Behaviors.same
      }
    }
  }
}
