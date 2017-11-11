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
}

object CheckoutProtocol {
  sealed trait DeliveryMethod
  case object Postman extends DeliveryMethod
  case object SelfPickup extends DeliveryMethod

  case class SelectDeliveryMethod(deliveryMethod: DeliveryMethod)

  sealed trait PaymentMethod
  case object CreditCard extends PaymentMethod
  case object OnlineTransfer extends PaymentMethod

  case class SelectPaymentMethod(paymentMethod: PaymentMethod)

  sealed trait CheckoutStatus
  case object CheckoutCancelled extends CheckoutStatus
  case object CheckoutClosed extends CheckoutStatus

  case object PaymentReceived
}

object PaymentProtocol {
  case object DoPayment
}