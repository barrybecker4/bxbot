package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.Market;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Holds information for an Exchange market to use in simulation.
 */
public final class StubMarket implements Market {

  private static final String MARKET_ID = "btc_usd";
  private static final String MARKET_NAME = "BTC_USD";

  private String id = MARKET_ID;
  private final String baseCurrency;
  private final String counterCurrency;

  /** Creates a new MarketImpl. */
  public StubMarket() {
    this.baseCurrency = "BTC";
    this.counterCurrency = "USD";
  }

  public String getName() {
    return MARKET_NAME;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public String getBaseCurrency() {
    return baseCurrency;
  }

  public String getCounterCurrency() {
    return counterCurrency;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StubMarket market = (StubMarket) o;
    return Objects.equal(id, market.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("name", MARKET_NAME)
            .add("id", id)
            .add("baseCurrency", baseCurrency)
            .add("counterCurrency", counterCurrency)
            .toString();
  }
}
