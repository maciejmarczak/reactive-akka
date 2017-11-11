package org.mmarczak.reactive.store

import org.scalatest.WordSpec

class CartSpec extends WordSpec {

  "A Cart object" should {

    "have a constructor" which {

      "is a no-arg, and creates a Cart with itemCount equal to 0" in {
        val cart: Cart = Cart()
        assert(cart.itemCount == 0)
      }

      "has one arg that specifies initial itemCount" in {
        val cart: Cart = Cart(5)
        assert(cart.itemCount == 5)
      }

    }

    "return a new Cart object with itemCount incremented by 1" in {
      val initialCart: Cart = Cart(3)
      val updatedCart: Cart = initialCart.addItem()

      assert(updatedCart.itemCount == 4)
      assert(initialCart != updatedCart)
    }

    "return a new Cart object with itemCount decremented by 1" in {
      val initialCart: Cart = Cart(3)
      val updatedCart: Cart = initialCart.removeItem()

      assert(updatedCart.itemCount == 2)
      assert(initialCart != updatedCart)
    }

  }

}
