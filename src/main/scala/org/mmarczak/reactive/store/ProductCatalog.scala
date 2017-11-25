package org.mmarczak.reactive.store

import akka.actor.{Actor, Props}
import org.mmarczak.reactive.store.ProductCatalogProtocol.FindItems

object ProductCatalog {
  def props(): Props = Props[ProductCatalog]
}

class ProductCatalog extends Actor {

  override def receive: Receive = {
    case FindItems(query) =>
      sender ! findItems(query)
  }

  private def findItems(query: String) = {
    query
  }
}
