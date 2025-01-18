package de.othr.dco.webshop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import de.othr.dco.webshop.Entities.{Item, Order, User}
import de.othr.dco.webshop.WebshopActor.AllItemsForUser

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
   * @param user    The user.
   * @param replyTo The requesting actor.
   */
  case class GetAllItemsForUser(user: User, replyTo: ActorRef[WebshopActor.WebshopAction]) extends BasketAction

  def apply(): Behavior[BasketAction] = handleBasket()

  private def handleBasket(basketList: List[Order] = List.empty): Behavior[BasketAction] = Behaviors.receive {
    (context, message) => {
      message match {
        case AddItemToUserBasket(user, item) =>
          // Update item where userId matches with userId from the message.
          var basketUpdated = if (!basketList.exists(_.user.id == user.id)) {
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
