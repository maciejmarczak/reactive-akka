package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import CartProtocol._, CheckoutProtocol._

import scala.concurrent.Await

object StoreApp extends App {
  val system = ActorSystem(Config.systemName)

  val cart = system.actorOf(CartFSM.props())

  cart ! AddItem
  cart ! AddItem
  cart ! AddItem
  cart ! RemoveItem
  cart ! StartCheckout

  implicit val timeout = Timeout(15, TimeUnit.SECONDS)
  val future = cart ? GetCheckout
  val checkout = Await.result(future, timeout.duration).asInstanceOf[ActorRef]

  checkout ! SelectDeliveryMethod(Postman)
  checkout ! SelectPaymentMethod(CreditCard)
  checkout ! ReceivePayment
}