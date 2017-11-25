package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorLogging, ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.persistence.PersistentActor
import org.mmarczak.reactive.store.CartProtocol._
import org.mmarczak.reactive.store.CheckoutProtocol.{CheckoutCancelled, CheckoutClosed, CheckoutStatus}
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted}

import scala.concurrent.duration.FiniteDuration

case class Item(id: String, name: String, count: Int)
case class Cart(items: Map[String, Item] = Map.empty) {
  def addItem(it: Item): Cart =
    copy(items = items.updated(it.id, it.copy(count = itemCount(it) + it.count)))

  def removeItem(it: Item): Cart =
    if (itemCount(it) > 0)
      copy(items = items.updated(it.id, it.copy(count = Math.max(0, itemCount(it) - it.count))))
    else
      copy(items = items - it.id)

  def allItemsCount(): Int = items.foldLeft(0)(_ + _._2.count)
  def itemCount(it: Item): Int = if (items contains it.id) items(it.id).count else 0
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
      case AddItem(item) => {
        state = state.addItem(item)
        log.info(s"Adding item to the cart. New item's count: ${state.allItemsCount}")
        CartTimer.refresh()
        nonEmpty
      }
      case RemoveItem(item) => {
        state = state.removeItem(item)
        log.info(s"Removing item from the cart. New item's count: ${state.allItemsCount}")
        CartTimer.refresh()
        if (state.allItemsCount == 0) empty else nonEmpty
      }
      case StartCheckout => {
        log.info("Starting checkout.")
        checkout = context.actorOf(CheckoutManager.props(), "checkout")
        inCheckout
      }
      case CartExpired => {
        state = Cart()
        log.info(s"Expired: ${state.allItemsCount}")
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
      if (state.allItemsCount > 0) {
        log.info(s"Refreshing timer: $CartExpired")
        timers.startSingleTimer(CartExpired, CartExpired, cartTimeout)
      }
    }
  }

  def empty: Receive = LoggingReceive {
    case AddItem(item) => persist(AddItem(item)) {
      addItem =>
        updateState(addItem)
      }
    case GetState => sender ! state
  }

  def nonEmpty: Receive = LoggingReceive {
    case AddItem(item) => persist(AddItem(item)) {
      addItem =>
        updateState(addItem)
    }
    case RemoveItem(item) => {
      persist(RemoveItem(item)) {
        removeItem => {
          updateState(removeItem)
          if (state.allItemsCount == 0) parent ! CartEmpty
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
