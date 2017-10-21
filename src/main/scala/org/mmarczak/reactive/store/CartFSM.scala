package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, FSM, Props}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, GetCheckout, RemoveItem, StartCheckout}
import org.mmarczak.reactive.store.CheckoutProtocol.{Cancelled, Closed}

import scala.concurrent.duration.FiniteDuration

sealed trait CartState
case object Empty extends CartState
case object NonEmpty extends CartState
case object InCheckout extends CartState

final case class CartData(itemCount: Int, checkoutActor: ActorRef = null)

object CartFSM {
  def props(): Props = Props[CartFSM]
}

class CartFSM extends FSM[CartState, CartData] {

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
      log.info("Starting checkout.")
      goto(InCheckout) using CartData(itemCount, context.actorOf(CheckoutFSM.props()))
    }
    case Event(StateTimeout, _) => {
      log.info("Cart timer expired.")
      goto(Empty) using CartData(0)
    }
  }

  when(InCheckout) {
    case Event(Closed, _) => {
      log.info("Checkout closed.")
      goto(Empty) using CartData(0)
    }
    case Event(Cancelled, CartData(itemCount, _)) => {
      log.info("Checkout cancelled.")
      goto(NonEmpty) using CartData(itemCount)
    }
    case Event(GetCheckout, cartData @ CartData(_, checkoutActor)) => {
      sender ! checkoutActor
      stay() using cartData
    }
  }

}
