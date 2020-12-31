package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;


/**
 * A MarketOrder implementation that can be used in simulation.
 */
public final class StubMarketOrder implements MarketOrder {

  private final OrderType type;
  private final BigDecimal price;
  private final BigDecimal quantity;
  private final BigDecimal total;

  /** Creates a new Market Order. */
  public StubMarketOrder(OrderType type, BigDecimal price) {
    this.type = type;
    this.price = price;
    this.quantity = new BigDecimal("1.0");
    this.total = quantity.multiply(price);
  }

  public OrderType getType() {
    return type;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getTotal() {
    return total;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", type)
        .add("price", price)
        .add("quantity", quantity)
        .add("total", total)
        .toString();
  }
}
