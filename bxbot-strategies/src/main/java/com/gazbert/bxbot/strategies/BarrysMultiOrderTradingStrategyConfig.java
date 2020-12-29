package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import java.math.BigDecimal;
import java.math.RoundingMode;


public class BarrysMultiOrderTradingStrategyConfig extends BaseStrategyConfig {

  /**
   * max number of outstanding sell orders that can be in existence at any one time.
   * If the market falls and never recovers, then this limits what we can lose.
   */
  private final Integer maxConcurrentSellOrders;

  /**
   * The amount to spend when buying. When selling the amound of counter currency will be greater.
   */
  private final BigDecimal counterCurrencyBuyOrderAmount;

  /**
   * If the market goes down by this percent, issue buy order at the current price.
   * Once that buy order is filled, issue a corresponding sell order at
   * the buy price * (1 +  percent-change-threshold/100).
   */
  private final BigDecimal percentChangeThreshold;


  public Integer getMaxConcurrentSellOrders() {
    return maxConcurrentSellOrders;
  }

  public BigDecimal getCounterCurrencyBuyOrderAmount() {
    return counterCurrencyBuyOrderAmount;
  }

  public BigDecimal getPercentChangeThreshold() {
    return percentChangeThreshold;
  }

  /**
   * Loads the config for the strategy. We expect the config items to be in the
   * {project-root}/config/strategies.yaml config file.
   *
   * @param config the config for the Trading Strategy.
   */
  public BarrysMultiOrderTradingStrategyConfig(IStrategyConfigItems config) {
    strategyId = config.getStrategyId();
    maxConcurrentSellOrders = retrieveMaxConcurrentSellOrders(config);
    counterCurrencyBuyOrderAmount = retrieveCounterCurrencyBuyOrderAmount(config);
    percentChangeThreshold = retrievePercentChangeThreshold(config);
  }

  private Integer retrieveMaxConcurrentSellOrders(IStrategyConfigItems config) {
    String value = getConfigValueAsString("max-concurrent-sell-orders", config);
    return Integer.parseInt(value);
  }

  private BigDecimal retrieveCounterCurrencyBuyOrderAmount(IStrategyConfigItems config) {
    String value = getConfigValueAsString("counter-currency-buy-order-amount", config);
    return new BigDecimal(value);
  }

  private BigDecimal retrievePercentChangeThreshold(IStrategyConfigItems config) {
    String value = getConfigValueAsString("percent-change-threshold", config);

    final BigDecimal percentChangeThresholdFromConfig =
            new BigDecimal(value);
    return percentChangeThresholdFromConfig
            .divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);
  }
}
