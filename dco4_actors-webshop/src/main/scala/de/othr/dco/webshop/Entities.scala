package de.othr.dco.webshop

object Entities {
  case class Item(name: String, price: Double)

  case class User(id: Int, name: String)

  case class Order(user: User, items: List[Item])

  case class Payment(order: Order, pricePaid: Double)
}
