package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, RemoveItem}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class CartSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  "A Cart actor" must {
    val cart = TestActorRef[Cart]
    val underlyingActor = cart.underlyingActor

    "start with item's count equal to 0" in {
      assert(underlyingActor.itemCount == 0)
    }

    "increment the item's count" in {
      cart ! AddItem
      assert(underlyingActor.itemCount == 1)
    }

    "decrement the item's count" in {
      cart ! RemoveItem
      assert(underlyingActor.itemCount == 0)
    }

    "not decrement the counter if it's already equal to 0" in {
      assert(underlyingActor.itemCount == 0)
      cart ! RemoveItem
      assert(underlyingActor.itemCount == 0)
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
