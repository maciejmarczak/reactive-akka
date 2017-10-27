package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, Props, Timers}

import scala.concurrent.duration.FiniteDuration

object Cart {
  def props(): Props = Props(new Cart())
}

class Cart extends Actor with ActorLogging with Timers {

  var itemCount: Int = 0

  def addItem(): Unit = {
    itemCount += 1
    log.info(s"Added item to the cart. New item count: $itemCount.")
    CartTimer.refresh()
  }

  def removeItem(): Unit = {
    itemCount -= 1
    log.info(s"Removed item from the cart. New item count: $itemCount.")
    CartTimer.refresh()
  }

  import context._
  import CartProtocol._, CheckoutProtocol._, CustomerProtocol._
  def emptyCart(): Unit = {
    itemCount = 0
    log.info("Removed all items from the cart. The cart is now empty.")
    parent ! CartEmpty
    become(empty)
  }

  private object CartTimer {
    case object CartExpired
    private lazy val cartTimeout = new FiniteDuration(Config.cartTimeout, TimeUnit.SECONDS)

    def refresh(): Unit = {
      timers.cancelAll()
      if (itemCount > 0) {
        timers.startSingleTimer(CartExpired, CartExpired, cartTimeout)
      }
    }
  }

  def empty: Receive = {
    case AddItem => {
      addItem()
      become(nonEmpty)
    }
  }

  def nonEmpty: Receive = {
    case AddItem => addItem()
    case RemoveItem if itemCount == 1 => emptyCart()
    case RemoveItem => removeItem()
    case StartCheckout => {
      parent ! CheckoutStarted(context.actorOf(Checkout.props(), "checkout"))
      become(inCheckout)
    }
    case CartTimer.CartExpired => emptyCart()
  }

  def inCheckout: Receive = {
    case CheckoutCancelled => {
      CartTimer.refresh()
      become(nonEmpty)
    }
    case CheckoutClosed => emptyCart()
  }

  // it's starting in an 'empty' state
  override def receive: Receive = empty
}
