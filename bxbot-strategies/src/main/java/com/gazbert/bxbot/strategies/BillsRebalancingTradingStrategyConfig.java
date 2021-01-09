package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import java.math.BigDecimal;


public class BillsRebalancingTradingStrategyConfig extends BaseStrategyConfig {

  /**
   * Don't rebalance unless at least this much value in counter currency is moved.
   */
  private final BigDecimal counterCurrencyThreshold;

  public BigDecimal getCounterCurrencyThreshold() {
    return counterCurrencyThreshold;
  }

  /**
   * Loads the config for the strategy. We expect the config items to be in the
   * {project-root}/config/strategies.yaml config file.
   *
   * @param config the config for the Trading Strategy.
   */
  public BillsRebalancingTradingStrategyConfig(IStrategyConfigItems config) {
    strategyId = config.getStrategyId();
    counterCurrencyThreshold = retrieveCounterCurrencyThreshold(config);
  }

  private BigDecimal retrieveCounterCurrencyThreshold(IStrategyConfigItems config) {
    String value = getConfigValueAsString("counter-currency-threshold", config);
    return new BigDecimal(value);
  }
}
