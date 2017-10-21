package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{FSM, Props}
import org.mmarczak.reactive.store.CheckoutProtocol._

import scala.concurrent.duration.FiniteDuration

sealed trait CheckoutState
case object SelectingDelivery extends CheckoutState
case object SelectingPayment extends CheckoutState
case object ProcessingPayment extends CheckoutState

final case class CheckoutData(deliveryMethod: DeliveryMethod, paymentMethod: PaymentMethod)

object CheckoutFSM {
  def props(): Props = Props[CheckoutFSM]
}

class CheckoutFSM extends FSM[CheckoutState, CheckoutData] {

  startWith(SelectingDelivery, CheckoutData(Postman, CreditCard))

  private case object CheckoutExpired

  when(SelectingDelivery) {
    case Event(SelectDeliveryMethod(method), CheckoutData(_, paymentMethod)) => {
      log.info(s"Delivery method selected: $method.")
      goto(SelectingPayment) using CheckoutData(method, paymentMethod)
    }
    case Event(Cancelled | CheckoutExpired, _) => {
      log.info("Checkout cancelled.")
      terminate(Cancelled)
    }
  }

  when(SelectingPayment) {
    case Event(SelectPaymentMethod(method), CheckoutData(deliveryMethod, _)) => {
      log.info(s"Payment method selected: $method")
      goto(ProcessingPayment) using CheckoutData(deliveryMethod, method)
    }
    case Event(Cancelled | CheckoutExpired, _) => {
      log.info("Checkout cancelled.")
      terminate(Cancelled)
    }
  }

  when(ProcessingPayment, stateTimeout = FiniteDuration(Config.paymentTimeout, TimeUnit.SECONDS)) {
    case Event(ReceivePayment, _) => {
      log.info("Payment received.")
      terminate(Closed)
    }
    case Event(Cancelled | StateTimeout, _) => {
      log.info("Checkout cancelled.")
      terminate(Cancelled)
    }
  }

  override def preStart(): Unit = {
    super.preStart()
    setTimer(CheckoutExpired.toString, CheckoutExpired,
      FiniteDuration(Config.checkoutTimeout, TimeUnit.SECONDS))
  }

  def terminate(checkoutStatus: CheckoutStatus): State = {
    log.info(s"Checkout: $checkoutStatus")
    context.parent ! checkoutStatus
    stop()
  }

}
