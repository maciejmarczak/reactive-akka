package org.mmarczak.reactive.store

import akka.actor.ActorRef

case object Shutdown

object CustomerProtocol {
  case class CheckoutStarted(actorRef: ActorRef)
  case class PaymentServiceStarted(actorRef: ActorRef)
  case object CartEmpty
  case object PaymentConfirmed
}

object CartProtocol {
  sealed trait Event
  case object AddItem extends Event
  case object RemoveItem extends Event
  case object StartCheckout extends Event
  case object CartExpired extends Event
  case object GetState
  case object GetCheckout
}

object CheckoutProtocol {
  sealed trait Event

  sealed trait DeliveryMethod
  case object Postman extends DeliveryMethod
  case object SelfPickup extends DeliveryMethod

  case class SelectDeliveryMethod(deliveryMethod: DeliveryMethod) extends Event

  sealed trait PaymentMethod
  case object CreditCard extends PaymentMethod
  case object OnlineTransfer extends PaymentMethod

  case class SelectPaymentMethod(paymentMethod: PaymentMethod) extends Event

  sealed trait CheckoutStatus
  case object CheckoutCancelled extends CheckoutStatus
  case object CheckoutClosed extends CheckoutStatus
  case object CheckoutExpired extends CheckoutStatus

  case object PaymentReceived extends Event
}

object PaymentProtocol {
  case object DoPayment
}