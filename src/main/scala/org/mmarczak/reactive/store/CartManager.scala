package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef, Props, Timers}
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

  override def persistenceId: String = id

  var checkout: ActorRef = _
  var state = Cart()

  import context._
  def updateState(event: CartProtocol.Event): Unit =
    become(event match {
      case AddItem => {
        state = state.addItem()
        log.info(s"Adding item to the cart. New item's count: ${state.itemCount}")
        CartTimer.refresh()
        nonEmpty
      }
      case RemoveItem => {
        state = state.removeItem()
        log.info(s"Removing item from the cart. New item's count: ${state.itemCount}")
        CartTimer.refresh()
        if (state.itemCount == 0) empty else nonEmpty
      }
      case StartCheckout => {
        log.info("Starting checkout.")
        checkout = context.actorOf(CheckoutManager.props(), "checkout")
        inCheckout
      }
      case CartExpired => {
        state = Cart()
        log.info(s"Expired: ${state.itemCount}")
        empty
      }
    })

  def updateState(checkoutStatus: CheckoutStatus): Unit =
    become(checkoutStatus match {
      case CheckoutCancelled => {
        log.info("Checkout cancelled.")
        CartTimer.refresh()
        nonEmpty
      }
      case CheckoutClosed => {
        log.info("Checkout closed.")
        state = Cart()
        empty
      }
    })

  val receiveRecover: Receive = {
    case event: CartProtocol.Event => updateState(event)
    case status: CheckoutStatus => updateState(status)
  }

  private object CartTimer {
    private lazy val cartTimeout = new FiniteDuration(Config.cartTimeout, TimeUnit.SECONDS)

    def refresh(): Unit = {
      timers.cancelAll()
      if (state.itemCount > 0) {
        log.info(s"Refreshing timer: $CartExpired")
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
        _ => {
          updateState(StartCheckout)
          parent ! CheckoutStarted(checkout)
        }
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
    case GetCheckout => sender ! checkout
  }

  // it's starting in an 'empty' state
  override def receiveCommand: Receive = empty
}
