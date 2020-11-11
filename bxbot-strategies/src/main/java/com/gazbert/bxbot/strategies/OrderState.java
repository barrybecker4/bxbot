package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.trading.api.OrderType;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;

/**
 * Models the state of an Order placed on the exchange.
 *
 * <p>Typically, you would maintain order state in a database or use some other persistence method
 * to recover from restarts and for audit purposes. In this example, we are storing the state in
 * memory to keep it simple.
 */
class OrderState {

  /**
   * Id - default to null.
   */
  public String id = null;

  /**
   * Type: buy/sell. We default to null which means no order has been placed yet, i.e. we've just
   * started!
   */
  public OrderType type = null;

  /**
   * Price to buy/sell at - default to zero.
   */
  public BigDecimal price = BigDecimal.ZERO;

  /**
   * Number of units to buy/sell - default to zero.
   */
  public BigDecimal amount = BigDecimal.ZERO;

  public OrderState() {
  }

  public OrderState(String id, OrderType type, BigDecimal price, BigDecimal amount) {
    this.id = id;
    this.type = type;
    this.price = price;
    this.amount = amount;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("id", id)
            .add("type", type)
            .add("price", price)
            .add("amount", amount)
            .toString();
  }
}