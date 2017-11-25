package org.mmarczak.reactive.store

import org.scalatest.WordSpec

class CartSpec extends WordSpec {

  "A Cart object" should {

    "correctly add up all item counts" in {
      val testMap = Map(
        "1" -> Item("1", "1st item", 1),
        "2" -> Item("2", "2nd item", 2),
        "3" -> Item("3", "3rd item", 3),
        "4" -> Item("4", "4th item", 4)
      )

      val cart: Cart = Cart(testMap)
      assert(cart.allItemsCount == 10)
    }

    "have a constructor" which {

      "is a no-arg, and creates a Cart with empty items map" in {
        val cart: Cart = Cart()
        assert(cart.allItemsCount == 0)
      }

      "has one arg that specifies initial items map" in {
        val cart: Cart = Cart(Map("1" -> Item("1", "Sample Item", 5)))
        assert(cart.allItemsCount == 5)
      }

    }

    "return a new Cart object with allItemsCount incremented" in {
      val initialCart: Cart = Cart(Map("1" -> Item("1", "Sample Item", 5)))
      val updatedCart: Cart = initialCart.addItem(Item("1", "Sample Item", 5))

      assert(updatedCart.allItemsCount == 10)
      assert(initialCart != updatedCart)
    }

    "return a new Cart object with allItemsCount decremented" in {
      val initialCart: Cart = Cart(Map("1" -> Item("1", "Sample Item", 5)))
      val updatedCart: Cart = initialCart.removeItem(Item("1", "Sample Item", 3))

      assert(updatedCart.allItemsCount == 2)
      assert(initialCart != updatedCart)
    }

    "return a new Cart object without given item" in {
      val initialCart: Cart = Cart(Map("1" -> Item("1", "Sample Item", 5)))
      val updatedCart: Cart = initialCart.removeItem(Item("1", "Sample Item", 5))

      assert(updatedCart.allItemsCount == 0)
      assert(initialCart != updatedCart)
    }

    "properly handle a negative decrement case" in {
      val initialCart: Cart = Cart(Map("1" -> Item("1", "Sample Item", 5)))
      val updatedCart: Cart = initialCart.removeItem(Item("1", "Sample Item", 7))

      assert(updatedCart.allItemsCount == 0)
      assert(initialCart != updatedCart)
    }

  }

}
