package org.mmarczak.reactive.store

object CartProtocol {
  case object AddItem
  case object RemoveItem
  case object StartCheckout
  case object GetCheckout
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
  case object Cancelled extends CheckoutStatus
  case object Closed extends CheckoutStatus

  case object ReceivePayment
}