package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import org.mmarczak.reactive.store.ProductCatalogProtocol.FindItems
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class ProductCatalogSpec extends TestKit(ActorSystem("ProductCatalogSpec"))
  with WordSpecLike with BeforeAndAfterAll with ImplicitSender {

  "A ProductCatalog actor" should {
    "return a list of unique items without duplicates" in {
      val productCatalog = TestActorRef[ProductCatalog]

      productCatalog ! FindItems("almonds jar")
      expectMsg(List(
        Item("0014113524759", "Sunkist California Almonds Jar Crytex"),
        Item("0014113533225", "Almonds 10 Sunkist"),
        Item("0014113533669", "Flavored Sliced Almonds Sunkist")
      ))
    }
  }
}
