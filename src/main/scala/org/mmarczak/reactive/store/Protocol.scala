package org.mmarczak.reactive.store

import akka.actor.ActorRef

object CustomerProtocol {
  case class CheckoutStarted(actorRef: ActorRef)
  case class PaymentServiceStarted(actorRef: ActorRef)
  case object CartEmpty
  case object PaymentConfirmed
}

object CartProtocol {
  case object AddItem
  case object RemoveItem
  case object StartCheckout
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