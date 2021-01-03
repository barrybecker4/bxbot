package com.gazbert.bxbot.strategies;

import java.util.Map;

/**
 * Encapsulates (optional) Strategy Config Items for simulated strategy.
 */
public final class BarrysMultiOrderStrategyConfigItems extends AbstractStrategyConfigItems {

  /**
   * Constructor.
   */
  public BarrysMultiOrderStrategyConfigItems() {
    this.strategyId = "barrys-multi-order-strategy";
    this.items = Map.of(
            "max-concurrent-sell-orders", "5",
            "counter-currency-buy-order-amount", "50",
            "percent-change-threshold", "4"
    );
  }
}
