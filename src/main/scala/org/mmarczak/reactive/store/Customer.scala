package org.mmarczak.reactive.store

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, StartCheckout}
import org.mmarczak.reactive.store.CheckoutProtocol._
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted, PaymentConfirmed, PaymentServiceStarted}
import org.mmarczak.reactive.store.PaymentProtocol.DoPayment

object Customer {
  def props(): Props = Props[Customer]
}

class Customer extends Actor with ActorLogging {

  private val cart = context.actorOf(CartManager.props(), "cart")

  private var checkout: ActorRef = _
  private var payment: ActorRef = _

  cart ! AddItem
  cart ! AddItem
  cart ! StartCheckout

  override def receive: Receive = {
    case CheckoutStarted(checkoutActor) => {
      checkout = checkoutActor
      checkout ! SelectDeliveryMethod(SelfPickup)
      checkout ! SelectPaymentMethod(OnlineTransfer)
    }
    case PaymentServiceStarted(paymentActor) => {
      payment = paymentActor
      payment ! DoPayment
    }
    case PaymentConfirmed => log.info("Payment confirmed.")
    case CartEmpty => log.info("Cart empty.")
    case CheckoutClosed => log.info("Checkout closed.")
  }

}
