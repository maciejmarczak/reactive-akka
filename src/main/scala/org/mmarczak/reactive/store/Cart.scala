package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import scala.concurrent.duration.FiniteDuration

object Cart {
  def props(): Props = Props(new Cart())
}

class Cart extends Actor with ActorLogging with Timers {

  private var itemCount = 0
  private var checkoutActor: ActorRef = _

  def addItem(): Unit = {
    itemCount += 1
    log.info(s"Added item to the cart. New item count: $itemCount.")
    CartTimer.resetTimer()
  }

  def removeItem(): Unit = {
    itemCount -= 1
    log.info(s"Removed item from the cart. New item count: $itemCount.")
    if (itemCount == 0) CartTimer.disableTimer() else CartTimer.resetTimer()
  }

  private object CartTimer {
    case object CartExpired
    private lazy val cartTimeout = new FiniteDuration(Config.cartTimeout, TimeUnit.SECONDS)

    def resetTimer(): Unit = {
      disableTimer()
      log.info(s"Starting new SingleTimer instance with timeout $cartTimeout.")
      timers.startSingleTimer(CartExpired, CartExpired, cartTimeout)
    }

    def disableTimer(): Unit = {
      log.info("Cancelling existing timers.")
      timers.cancelAll()
    }

  }

  import context._
  import CartProtocol._, CheckoutProtocol._
  def empty: Receive = {
    case AddItem => {
      addItem()
      log.info("Switching state from empty to nonEmpty.")
      become(nonEmpty)
    }
  }

  def nonEmpty: Receive = {
    case AddItem => addItem()
    case RemoveItem if itemCount == 1 => {
      removeItem()
      log.info("Switching state from nonEmpty to empty.")
      become(empty)
    }
    case RemoveItem => removeItem()
    case StartCheckout => {
      log.info("Switching state from nonEmpty to inCheckout.")
      CartTimer.disableTimer()
      checkoutActor = context.actorOf(Checkout.props())
      become(inCheckout)
    }
    case CartTimer.CartExpired => {
      itemCount = 0
      log.info("Cart expired. Switching state from nonEmpty to empty.")
      become(empty)
    }
  }

  def inCheckout: Receive = {
    case Cancelled => {
      log.info("Checkout cancelled.")
      CartTimer.resetTimer()
      become(nonEmpty)
    }
    case Closed => {
      log.info("Checkout closed.")
      itemCount = 0
      become(empty)
    }
    case GetCheckout => sender ! checkoutActor
  }

  // it's starting in an 'empty' state
  override def receive: Receive = empty
}
