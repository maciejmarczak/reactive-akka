package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props, Timers}

import scala.concurrent.duration.FiniteDuration
import CheckoutProtocol._
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import org.mmarczak.reactive.store.CustomerProtocol.PaymentServiceStarted

case class Checkout(
  deliveryMethod: DeliveryMethod = Postman,
  paymentMethod: PaymentMethod= CreditCard
)

object CheckoutManager {
  def props(id: String = "checkout-manager"): Props = Props(new CheckoutManager(id))
}

class CheckoutManager(id: String) extends PersistentActor with ActorLogging with Timers {

  override def persistenceId: String = id

  var state = Checkout()

  private object CheckoutTimer {
    lazy val checkoutTimeout = new FiniteDuration(Config.checkoutTimeout, TimeUnit.SECONDS)
    lazy val paymentTimeout = new FiniteDuration(Config.paymentTimeout, TimeUnit.SECONDS)

    case object PaymentExpired
  }

  import context._
  import CheckoutTimer._
  def updateState(event: CheckoutProtocol.Event): Unit =
    become(event match {
      case SelectDeliveryMethod(method) => {
        log.info(s"Changing delivery method from ${state.deliveryMethod} to $method.")
        state = Checkout(method)
        selectingPayment
      }
      case SelectPaymentMethod(method) => {
        log.info(s"Changing payment method from ${state.paymentMethod} to $method.")
        state = Checkout(state.deliveryMethod, method)
        setTimer(PaymentExpired, paymentTimeout)
        processingPayment
      }
    })

  def updateState(checkoutStatus: CheckoutStatus): Unit = {
    log.info(s"Checkout: $checkoutStatus")
    context.parent ! checkoutStatus
    context.stop(self)
  }

  val receiveRecover: Receive = {
    case event: CheckoutProtocol.Event => updateState(event)
    case status: CheckoutStatus => updateState(status)
  }

  def selectingDelivery: Receive = LoggingReceive {
    case SelectDeliveryMethod(method) => {
      persist(SelectDeliveryMethod(method)) {
        deliveryMethod => updateState(deliveryMethod)
      }
    }
    case CheckoutCancelled | CheckoutExpired => {
      persist(CheckoutCancelled) {
        _ => updateState(CheckoutCancelled)
      }
    }
  }

  def selectingPayment: Receive = LoggingReceive {
    case SelectPaymentMethod(method) => {
      persist(SelectPaymentMethod(method)) {
        paymentMethod => {
          updateState(paymentMethod)
          sender ! PaymentServiceStarted(context.actorOf(PaymentService.props(), "paymentService"))
        }
      }
    }
    case CheckoutCancelled | CheckoutExpired => {
      persist(CheckoutCancelled) {
        _ => updateState(CheckoutCancelled)
      }
    }
  }

  def processingPayment: Receive = LoggingReceive {
    case PaymentReceived => {
      persist(PaymentReceived) {
        _ => updateState(CheckoutClosed)
      }
    }
    case CheckoutCancelled | PaymentExpired => {
      persist(CheckoutCancelled) {
        _ => updateState(CheckoutCancelled)
      }
    }
  }

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

  // it's starting in an 'selectingDelivery' state
  override def receiveCommand: Receive = selectingDelivery
}
