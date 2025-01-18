package de.othr.dco.webshop

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, Behavior}
import de.othr.dco.webshop.Entities.{Item, User}
import de.othr.dco.webshop.WebshopActor.{AddItemToBasket, MakeOrder}

import scala.language.postfixOps

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
