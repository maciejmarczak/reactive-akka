package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{FSM, LoggingFSM, Props}
import org.mmarczak.reactive.store.CheckoutProtocol._

import scala.concurrent.duration.FiniteDuration

sealed trait CheckoutState
case object SelectingDelivery extends CheckoutState
case object SelectingPayment extends CheckoutState
case object ProcessingPayment extends CheckoutState

final case class CheckoutData(deliveryMethod: DeliveryMethod, paymentMethod: PaymentMethod)

@Deprecated
object CheckoutFSM {
  def props(): Props = Props[CheckoutFSM]
}

@Deprecated
class CheckoutFSM extends FSM[CheckoutState, CheckoutData] with LoggingFSM[CheckoutState, CheckoutData] {

  startWith(SelectingDelivery, CheckoutData(Postman, CreditCard))

  private case object CheckoutExpired

  when(SelectingDelivery) {
    case Event(SelectDeliveryMethod(method), CheckoutData(_, paymentMethod)) => {
      log.info(s"Delivery method selected: $method.")
      goto(SelectingPayment) using CheckoutData(method, paymentMethod)
    }
    case Event(CheckoutCancelled | CheckoutExpired, _) => {
      terminate(CheckoutCancelled)
    }
  }

  when(SelectingPayment) {
    case Event(SelectPaymentMethod(method), CheckoutData(deliveryMethod, _)) => {
      log.info(s"Payment method selected: $method")
      goto(ProcessingPayment) using CheckoutData(deliveryMethod, method)
    }
    case Event(CheckoutCancelled | CheckoutExpired, _) => {
      terminate(CheckoutCancelled)
    }
  }

  when(ProcessingPayment, stateTimeout = FiniteDuration(Config.paymentTimeout, TimeUnit.SECONDS)) {
    case Event(PaymentReceived, _) => {
      terminate(CheckoutClosed)
    }
    case Event(CheckoutCancelled | StateTimeout, _) => {
      terminate(CheckoutCancelled)
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
