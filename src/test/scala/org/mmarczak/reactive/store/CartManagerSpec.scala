package org.mmarczak.reactive.store

import java.util.concurrent.TimeUnit

import akka.pattern.gracefulStop
import akka.actor.{ActorSystem, PoisonPill}
import akka.persistence.inmemory.extension.{InMemoryJournalStorage, StorageExtension}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import org.mmarczak.reactive.store.CartProtocol.{AddItem, GetState, RemoveItem, StartCheckout}
import org.mmarczak.reactive.store.CheckoutProtocol._
import org.mmarczak.reactive.store.CustomerProtocol.{CartEmpty, CheckoutStarted}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.{Await, Future}

class CartManagerSpec extends TestKit(ActorSystem("CartSpec"))
  with WordSpecLike with BeforeAndAfterAll with BeforeAndAfterEach with ImplicitSender {

  override protected def beforeEach(): Unit = {
    TestProbe().send(StorageExtension(system).journalStorage, InMemoryJournalStorage.ClearJournal)
  }

  "A CartManager actor" must {

    "create and return a reference to Checkout actor" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(CartManager.props())

      customer.send(cart, AddItem)
      customer.send(cart, StartCheckout)
      customer.expectMsgPF() {
        case CheckoutStarted(_) => ()
      }
    }

    "send a CartEmpty message when item's count decrements to 0" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(CartManager.props())

      customer.send(cart, AddItem)
      customer.send(cart, RemoveItem)
      customer.expectMsg(CartEmpty)
    }

    "send a CartEmpty message on CartExpired" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(CartManager.props())

      customer.send(cart, AddItem)
      // expires after one second
      customer.expectMsg(CartEmpty)
    }

    "send a CartEmpty message on CheckoutClosed" in {
      val customer = TestProbe()
      val cart = customer.childActorOf(CartManager.props())

      customer.send(cart, AddItem)
      customer.send(cart, StartCheckout)
      customer.send(cart, CheckoutClosed)

      customer.expectMsgPF() {
        case CheckoutStarted(_) => ()
      }
      customer.expectMsg(CartEmpty)
    }

    import scala.concurrent.duration._

    "restore it's state" when {

      "it comes to item's count" in {
        val cart = system.actorOf(CartManager.props())

        cart ! AddItem
        cart ! GetState
        expectMsg(Cart(1))

        val stopped: Future[Boolean] = gracefulStop(cart, 2 seconds)
        Await.result(stopped, 3 seconds)

        val newCart = system.actorOf(CartManager.props())
        newCart ! GetState

        expectMsg(Cart(1))
      }

      "it comes to cart timer" in {
        val customer = TestProbe()
        val cart = customer.childActorOf(CartManager.props())

        customer.send(cart, AddItem)
        customer.send(cart, GetState)
        customer.expectMsg(Cart(1))

        val stopped: Future[Boolean] = gracefulStop(cart, 2 seconds)
        Await.result(stopped, 3 seconds)

        val newCart = customer.childActorOf(CartManager.props())

        customer.send(newCart, GetState)
        customer.expectMsg(Cart(1))
        customer.expectMsg(CartEmpty)
      }
    }
  }

  override def afterAll(): Unit = TestKit.shutdownActorSystem(system)

}
