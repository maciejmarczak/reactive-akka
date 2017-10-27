package org.mmarczak.reactive.store

import akka.actor.{Actor, ActorRef, Props}
import org.mmarczak.reactive.store.CustomerProtocol.{CheckoutStarted, PaymentServiceStarted}

object Customer {
  def props(): Props = Props[Customer]
}

class Customer extends Actor {

  private val cart = context.actorOf(Cart.props(), "cart")

  private var checkout: Option[ActorRef] = None
  private var payment: Option[ActorRef] = None

  override def receive: Receive = {
    case CheckoutStarted(checkoutActor) => checkout = Some(checkoutActor)
    case PaymentServiceStarted(paymentActor) => payment = Some(paymentActor)
  }

}
