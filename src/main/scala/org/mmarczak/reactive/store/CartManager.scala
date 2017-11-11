package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import org.mmarczak.reactive.store.CartProtocol._
import org.mmarczak.reactive.store.CheckoutProtocol.{CheckoutCancelled, CheckoutClosed, CheckoutStatus}
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted}

import scala.concurrent.duration.FiniteDuration

case class Cart(private val _itemCount: Int = 0) {
  def addItem(): Cart = Cart(_itemCount + 1)
  def removeItem(): Cart = Cart(Math.max(_itemCount - 1, 0))
  def itemCount(): Int = _itemCount
}

object CartManager {
  def props(id: String = "cart-manager"): Props = Props(new CartManager(id))
}

class CartManager(id: String) extends PersistentActor with ActorLogging with Timers {

  override def persistenceId = id

  var state = Cart()

  import context._
  def updateState(event: CartProtocol.Event): Unit =
    become(event match {
      case AddItem => {
        state = state.addItem()
        CartTimer.refresh()
        nonEmpty
      }
      case RemoveItem => {
        state = state.removeItem()
        CartTimer.refresh()
        if (state.itemCount == 0) empty else nonEmpty
      }
      case StartCheckout => inCheckout
      case CartExpired => {
        state = Cart()
        log.info(s"Expired: ${state.itemCount}")
        empty
      }
    })

  def updateState(checkoutStatus: CheckoutStatus): Unit =
    become(checkoutStatus match {
      case CheckoutCancelled => {
        CartTimer.refresh()
        nonEmpty
      }
      case CheckoutClosed => {
        state = Cart()
        empty
      }
    })

  val receiveRecover: Receive = {
    case event: CartProtocol.Event => updateState(event)
  }

  private object CartTimer {
    private lazy val cartTimeout = new FiniteDuration(Config.cartTimeout, TimeUnit.SECONDS)

    def refresh(): Unit = {
      timers.cancelAll()
      if (state.itemCount > 0) {
        timers.startSingleTimer(CartExpired, CartExpired, cartTimeout)
      }
    }
  }

  def empty: Receive = LoggingReceive {
    case AddItem => persist(AddItem) {
      addItem =>
        updateState(addItem)
      }
    case GetState => sender ! state
  }

  def nonEmpty: Receive = LoggingReceive {
    case AddItem => persist(AddItem) {
      addItem =>
        updateState(addItem)
    }
    case RemoveItem => {
      persist(RemoveItem) {
        removeItem => {
          updateState(removeItem)
          if (state.itemCount == 0) parent ! CartEmpty
        }
      }
    }
    case StartCheckout =>
      persist(StartCheckout) {
        _ => parent ! CheckoutStarted(context.actorOf(Checkout.props(), "checkout"))
    }
    case CartExpired =>
      persist(CartExpired) {
        cartExpired => {
          updateState(cartExpired)
          parent ! CartEmpty
        }
      }
    case GetState => sender ! state
  }

  def inCheckout: Receive = LoggingReceive {
    case CheckoutCancelled => persist(CheckoutCancelled) {
      checkoutCancelled =>
        updateState(checkoutCancelled)
    }
    case CheckoutClosed => persist(CheckoutClosed) {
      checkoutClosed => {
        updateState(checkoutClosed)
        parent ! CartEmpty
      }
    }
    case GetState => sender ! state
  }

  // it's starting in an 'empty' state
  override def receiveCommand: Receive = empty
}
