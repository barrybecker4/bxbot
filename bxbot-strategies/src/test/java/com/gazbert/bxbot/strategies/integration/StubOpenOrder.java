package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import java.math.BigDecimal;
import java.util.Date;

/**
 * A OpenOrder implementation that can be used by simulated Exchange Adapters.
 *
 */
public final class StubOpenOrder implements OpenOrder {

  private final String id;
  private final String marketId;
  private final OrderType type;
  private final BigDecimal price;
  private final BigDecimal quantity;

  /** Creates a new Open Order. */
  public StubOpenOrder(
      String id,
      String marketId,
      OrderType type,
      BigDecimal price,
      BigDecimal quantity) {

    this.id = id;
    this.marketId = marketId;
    this.type = type;
    this.price = price;
    this.quantity = quantity;
  }

  public String getId() {
    return id;
  }

  /** Returns the Order creation date. */
  public Date getCreationDate() {
    return null;
  }

  public String getMarketId() {
    return marketId;
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

  public BigDecimal getOriginalQuantity() {
    return null;
  }

  public BigDecimal getTotal() {
    return price.multiply(quantity);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StubOpenOrder openOrder = (StubOpenOrder) o;
    return Objects.equal(id, openOrder.id)
        && Objects.equal(marketId, openOrder.marketId)
        && type == openOrder.type;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id, marketId, type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("id", id)
        .add("marketId", marketId)
        .add("type", type)
        .add("price", price)
        .add("quantity", quantity)
        .toString();
  }
}
