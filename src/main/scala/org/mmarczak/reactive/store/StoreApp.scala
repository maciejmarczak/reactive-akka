package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import org.mmarczak.reactive.store.CartProtocol.{AddItem, RemoveItem}

object StoreApp extends App {
  val system = ActorSystem(Config.systemName)
  val customer = system.actorOf(Customer.props())
}