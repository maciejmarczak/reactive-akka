package org.mmarczak.reactive.store

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.mmarczak.reactive.store.CheckoutProtocol.CheckoutClosed
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted, PaymentConfirmed, PaymentServiceStarted}

object Customer {
  def props(): Props = Props[Customer]
}

class Customer extends Actor with ActorLogging {

  private val cart = context.actorOf(CartManager.props(), "cart")

  private var checkout: Option[ActorRef] = None
  private var payment: Option[ActorRef] = None

  override def receive: Receive = {
    case CheckoutStarted(checkoutActor) => checkout = Some(checkoutActor)
    case PaymentServiceStarted(paymentActor) => payment = Some(paymentActor)
    case PaymentConfirmed => log.info("Payment confirmed.")
    case CartEmpty => log.info("Cart empty.")
    case CheckoutClosed => log.info("Checkout closed.")
  }

}
