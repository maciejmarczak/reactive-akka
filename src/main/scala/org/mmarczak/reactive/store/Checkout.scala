package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit
import akka.actor.{Actor, ActorLogging, Props, Timers}
import scala.concurrent.duration.FiniteDuration

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

object Checkout {
  def props(): Props = Props(new Checkout())
}

class Checkout extends Actor with ActorLogging with Timers {

  private var deliveryMethod: DeliveryMethod = Postman
  private var paymentMethod: PaymentMethod = CreditCard

  private object CheckoutTimer {
    lazy val checkoutTimeout = new FiniteDuration(Config.checkoutTimeout, TimeUnit.SECONDS)
    lazy val paymentTimeout = new FiniteDuration(Config.paymentTimeout, TimeUnit.SECONDS)

    case object CheckoutExpired
    case object PaymentExpired
  }

  import context._
  import CheckoutTimer._
  def selectingDelivery: Receive = {
    case SelectDeliveryMethod(method) => {
      log.info(s"Changing delivery method from $deliveryMethod to $method.")
      deliveryMethod = method
      become(selectingPayment)
    }
    case Cancelled | CheckoutExpired => {
      stop(Cancelled)
    }
  }

  def selectingPayment: Receive = {
    case SelectPaymentMethod(method) => {
      log.info(s"Changing payment method from $paymentMethod to $method.")
      paymentMethod = method
      setTimer(PaymentExpired, paymentTimeout)
      become(processingPayment)
    }
    case Cancelled | CheckoutExpired => {
      stop(Cancelled)
    }
  }

  def processingPayment: Receive = {
    case ReceivePayment => {
      log.info("Payment received. Closing checkout.")
      stop(Closed)
    }
    case Cancelled | PaymentExpired => {
      stop(Cancelled)
    }
  }

  override def receive: Receive = selectingDelivery

  def stop(checkoutStatus: CheckoutStatus): Unit = {
    log.info(s"Checkout: $checkoutStatus")
    context.parent ! checkoutStatus
    context.stop(self)
  }

  def setTimer(timer: Any, timeout: FiniteDuration): Unit = {
    log.info(s"Starting new $timer timer with timeout $timeout")
    timers.startSingleTimer(timer, timer, timeout)
  }

  override def preStart(): Unit = {
    setTimer(CheckoutExpired, checkoutTimeout)
  }
}
