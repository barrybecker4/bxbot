package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import java.math.BigDecimal;
import java.math.RoundingMode;


public class BarrysTradingStrategyConfig extends BaseStrategyConfig {

  /**
   * The counter currency amount to use when placing the buy order. This was loaded from the
   * strategy entry in the {project-root}/config/strategies.yaml config file.
   */
  private final BigDecimal counterCurrencyBuyOrderAmount;

  /**
   * The minimum % gain was to achieve before placing a SELL oder. This was loaded from the strategy
   * entry in the {project-root}/config/strategies.yaml config file.
   */
  private final BigDecimal minimumPercentageGain;

  public BigDecimal getCounterCurrencyBuyOrderAmount() {
    return counterCurrencyBuyOrderAmount;
  }

  public BigDecimal getMinimumPercentageGain() {
    return minimumPercentageGain;
  }

  /**
   * Loads the config for the strategy. We expect the 'counter-currency-buy-order-amount' and
   * 'minimum-percentage-gain' config items to be present in the
   * {project-root}/config/strategies.yaml config file.
   *
   * @param config the config for the Trading Strategy.
   */
  public BarrysTradingStrategyConfig(IStrategyConfigItems config) {
    strategyId = config.getStrategyId();
    counterCurrencyBuyOrderAmount = retrieveCounterCurrencyBuyOrderAmount(config);
    minimumPercentageGain = retrieveMinimumPercentageGainAmount(config);
  }

  private BigDecimal retrieveCounterCurrencyBuyOrderAmount(IStrategyConfigItems config) {
    String value = getConfigValueAsString("counter-currency-buy-order-amount", config);
    return new BigDecimal(value);
  }

  private BigDecimal retrieveMinimumPercentageGainAmount(IStrategyConfigItems config) {

    String value = getConfigValueAsString("minimum-percentage-gain", config);

    // Will fail fast if value is not a number
    final BigDecimal minimumPercentageGainFromConfig =
            new BigDecimal(value);
    return minimumPercentageGainFromConfig
            .divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);
  }
}
