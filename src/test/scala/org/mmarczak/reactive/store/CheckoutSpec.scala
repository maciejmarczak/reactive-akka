package org.mmarczak.reactive.store

import akka.actor.ActorSystem
import akka.testkit.{TestActorRef, TestKit, TestProbe}
import org.mmarczak.reactive.store.CheckoutProtocol._
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class CheckoutSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll {

  "A Checkout actor" must {
    val checkout = TestActorRef[Checkout]
    val underlyingActor = checkout.underlyingActor

    "select delivery method" in {
      checkout ! SelectDeliveryMethod(SelfPickup)
      assert(underlyingActor.deliveryMethod == SelfPickup)
    }

    "select payment method" in {
      checkout ! SelectPaymentMethod(OnlineTransfer)
      assert(underlyingActor.paymentMethod == OnlineTransfer)
    }

  }

  "A Checkout actor" must {

    "send 'CheckoutClosed' notification to parent" in {
      val cart = TestProbe()
      val checkout = cart.childActorOf(Checkout.props())

      checkout ! SelectDeliveryMethod(Postman)
      checkout ! SelectPaymentMethod(CreditCard)
      checkout ! PaymentReceived
      cart.expectMsg(CheckoutClosed)
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
