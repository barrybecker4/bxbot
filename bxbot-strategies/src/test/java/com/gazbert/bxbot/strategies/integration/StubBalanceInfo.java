package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.util.Map;


/**
 * A BalanceInfo implementation that can be used in simulated Exchange Adapters.
 */
public final class StubBalanceInfo implements BalanceInfo {

  private final Map<String, BigDecimal> balancesAvailable;

  public StubBalanceInfo(
      Map<String, BigDecimal> balancesAvailable) {
    this.balancesAvailable = balancesAvailable;
  }

  public Map<String, BigDecimal> getBalancesAvailable() {
    return balancesAvailable;
  }


  public Map<String, BigDecimal> getBalancesOnHold() {
    return null;
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("balancesAvailable", balancesAvailable)
        .toString();
  }
}
