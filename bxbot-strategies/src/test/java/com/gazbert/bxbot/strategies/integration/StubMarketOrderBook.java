package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OrderType;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;


/**
 * Fake order book.
 * Given a price it will create a fake buy (bid) order that is slightly lower
 * and fake sell (ask) order that is slightly higher.
 */
public class StubMarketOrderBook implements MarketOrderBook {

  private static final BigDecimal ONE = new BigDecimal("1");
  private static final BigDecimal EPS = new BigDecimal("0.0001");

  private final String marketId;
  private final BigDecimal price;

  public StubMarketOrderBook(String marketId, Double price) {
    this.marketId = marketId;
    this.price = new BigDecimal(price);
  }

  @Override
  public String getMarketId() {
    return this.marketId;
  }

  // high, ask
  @Override
  public List<MarketOrder> getSellOrders() {

    MarketOrder order = new StubMarketOrder(OrderType.SELL, price.multiply(ONE.add(EPS)));
    return Collections.singletonList(order);
  }

  // low, bid
  @Override
  public List<MarketOrder> getBuyOrders() {
    MarketOrder order = new StubMarketOrder(OrderType.BUY, price.multiply(ONE.subtract(EPS)));
    return Collections.singletonList(order);
  }
}
