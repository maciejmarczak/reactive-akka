package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, RemoveItem, StartCheckout}
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted}
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
      cart ! RemoveItem
      assert(underlyingActor.itemCount == 0)
    }
  }

  "A Cart actor" must {

    "create and return a reference to Checkout actor" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(Cart.props())

      customer.send(cart, AddItem)
      customer.send(cart, StartCheckout)
      customer.expectMsgPF() {
        case CheckoutStarted(_) => ()
      }
    }

    "send an CartEmpty message when item's count decrement to 0" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(Cart.props())

      customer.send(cart, AddItem)
      customer.send(cart, RemoveItem)
      customer.expectMsg(CartEmpty)
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
