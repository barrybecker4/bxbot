package com.gazbert.bxbot.strategies;

import java.util.Map;

/**
 * Encapsulates (optional) Strategy Config Items for simulated strategy.
 */
public final class BillsRebalancingStrategyConfigItems extends AbstractStrategyConfigItems {

  /**
   * Constructor.
   */
  public BillsRebalancingStrategyConfigItems() {
    this.strategyId = "bills-rebalancing-strategy";
    items = Map.of(
            "counter-currency-threshold", "100"
    );
  }
}
