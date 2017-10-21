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
      timers.startSingleTimer(CartExpired, CartExpired, cartTimeout)
    }

    def disableTimer(): Unit = {
      timers.cancelAll()
    }

  }

  import context._
  import CartProtocol._, CheckoutProtocol._
  def empty: Receive = {
    case AddItem => {
      addItem()
      become(nonEmpty)
    }
  }

  def nonEmpty: Receive = {
    case AddItem => addItem()
    case RemoveItem if itemCount == 1 => {
      removeItem()
      become(empty)
    }
    case RemoveItem => removeItem()
    case StartCheckout => {
      CartTimer.disableTimer()
      checkoutActor = context.actorOf(Checkout.props())
      become(inCheckout)
    }
    case CartTimer.CartExpired => {
      itemCount = 0
      become(empty)
    }
  }

  def inCheckout: Receive = {
    case Cancelled => {
      CartTimer.resetTimer()
      become(nonEmpty)
    }
    case Closed => {
      itemCount = 0
      become(empty)
    }
    case GetCheckout => sender ! checkoutActor
  }

  // it's starting in an 'empty' state
  override def receive: Receive = empty
}
