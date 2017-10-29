package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}

import scala.concurrent.duration.FiniteDuration
import CheckoutProtocol._
import org.mmarczak.reactive.store.CustomerProtocol.PaymentServiceStarted

object Checkout {
  def props(): Props = Props(new Checkout())
}

class Checkout extends Actor with ActorLogging with Timers {

  var deliveryMethod: DeliveryMethod = Postman
  var paymentMethod: PaymentMethod = CreditCard

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
    case CheckoutCancelled | CheckoutExpired => {
      stop(CheckoutCancelled)
    }
  }

  def selectingPayment: Receive = {
    case SelectPaymentMethod(method) => {
      log.info(s"Changing payment method from $paymentMethod to $method.")
      paymentMethod = method
      sender ! PaymentServiceStarted(context.actorOf(PaymentService.props(), "paymentService"))
      setTimer(PaymentExpired, paymentTimeout)
      become(processingPayment)
    }
    case CheckoutCancelled | CheckoutExpired => {
      stop(CheckoutCancelled)
    }
  }

  def processingPayment: Receive = {
    case PaymentReceived => {
      stop(CheckoutClosed)
    }
    case CheckoutCancelled | PaymentExpired => {
      stop(CheckoutCancelled)
    }
  }

  override def receive: Receive = selectingDelivery

  def stop(checkoutStatus: CheckoutStatus): Unit = {
    log.info(s"Checkout: $checkoutStatus")
    context.parent ! checkoutStatus
    context.stop(self)
  }

  def setTimer(timer: Any, timeout: FiniteDuration): Unit = {
    timers.startSingleTimer(timer, timer, timeout)
  }

  override def preStart(): Unit = {
    setTimer(CheckoutExpired, checkoutTimeout)
  }
}
