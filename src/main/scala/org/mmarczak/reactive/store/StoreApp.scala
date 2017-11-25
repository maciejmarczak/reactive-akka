package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import org.mmarczak.reactive.store.ProductCatalogProtocol.FindItems

import scala.concurrent.Await
import scala.concurrent.duration._

object StoreApp extends App {

  val catalogSystem = ActorSystem("catalog", Config.catalogConfig)
  catalogSystem.actorOf(ProductCatalog.props(), "product-catalog")

  val localSystem = ActorSystem("local", Config.localConfig)
  val productCatalog = localSystem.actorSelection("akka.tcp://catalog@127.0.0.1:2555/user/product-catalog")

  implicit val timeout: Timeout = Timeout(10 seconds)
  val future = productCatalog ? FindItems("california almonds jar")
  val result = Await.result(future, timeout.duration)

  println("result: " + result)
}