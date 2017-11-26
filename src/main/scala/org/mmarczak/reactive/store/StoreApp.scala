package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.mmarczak.reactive.store.CartProtocol.{AddItem, StartCheckout}
import org.mmarczak.reactive.store.ProductCatalogProtocol.FindItems

import scala.concurrent.Await
import scala.concurrent.duration._

object StoreApp extends App {

  runCatalogSystem

  val localSystem = ActorSystem("local", Config.localConfig)
  val productCatalog = localSystem.actorSelection("akka.tcp://catalog@127.0.0.1:2555/user/product-catalog")
  val customer = localSystem.actorOf(Customer.props(), "customer")

  implicit val timeout: Timeout = Timeout(15 seconds)
  val future = productCatalog ? FindItems("california almonds jar")
  val result: List[Item] = Await.result(future, timeout.duration).asInstanceOf[List[Item]]

  result.foreach(println)

  customer ! AddItem(result.head.copy(count = 2))
  customer ! StartCheckout

  def runCatalogSystem = {
    val catalogSystem = ActorSystem("catalog", Config.catalogConfig)
    catalogSystem.actorOf(ProductCatalog.props(), "product-catalog")
  }
}