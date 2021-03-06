package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, FSM, LoggingFSM, Props}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, RemoveItem, StartCheckout}
import org.mmarczak.reactive.store.CheckoutProtocol.{CheckoutCancelled, CheckoutClosed}

import scala.concurrent.duration.FiniteDuration

sealed trait CartState
case object Empty extends CartState
case object NonEmpty extends CartState
case object InCheckout extends CartState

final case class CartData(itemCount: Int, checkoutActor: ActorRef = null)

@Deprecated
object CartFSM {
  def props(): Props = Props[CartFSM]
}

@Deprecated
class CartFSM extends FSM[CartState, CartData] with LoggingFSM[CartState, CartData] {

  startWith(Empty, CartData(0))

  when(Empty) {
    case Event(AddItem, _) => {
      log.info(s"Added item to the cart. New item count: 1.")
      goto(NonEmpty) using CartData(1)
    }
  }

  when(NonEmpty, stateTimeout = FiniteDuration(Config.cartTimeout, TimeUnit.SECONDS)) {
    case Event(AddItem, CartData(itemCount, _)) => {
      log.info(s"Added item to the cart. New item count: ${itemCount + 1}.")
      stay() using CartData(itemCount + 1)
    }
    case Event(RemoveItem, CartData(1, _)) => {
      log.info("Removed item from the cart. New item count: 0")
      goto(Empty) using CartData(0)
    }
    case Event(RemoveItem, CartData(itemCount, _)) => {
      log.info(s"Removed item from the cart. New item count: ${itemCount - 1}")
      stay() using CartData(itemCount - 1)
    }
    case Event(StartCheckout, CartData(itemCount, _)) => {
      goto(InCheckout) using CartData(itemCount, context.actorOf(CheckoutFSM.props()))
    }
    case Event(StateTimeout, _) => {
      goto(Empty) using CartData(0)
    }
  }

  when(InCheckout) {
    case Event(CheckoutClosed, _) => {
      goto(Empty) using CartData(0)
    }
    case Event(CheckoutCancelled, CartData(itemCount, _)) => {
      goto(NonEmpty) using CartData(itemCount)
    }
  }

}
