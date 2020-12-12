package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BarrysTradingStrategyConfig {

  private static final Logger LOG = LogManager.getLogger();

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
  public BarrysTradingStrategyConfig(StrategyConfig config) {

    counterCurrencyBuyOrderAmount = retrieveCounterCurrencyBuyOrderAmount(config);
    minimumPercentageGain = retrieveMinimumPercentageGainAmount(config);
  }

  private BigDecimal retrieveCounterCurrencyBuyOrderAmount(StrategyConfig config) {
    // Get counter currency buy amount...
    final String counterCurrencyBuyOrderAmountFromConfigAsString =
            config.getConfigItem("counter-currency-buy-order-amount");

    if (counterCurrencyBuyOrderAmountFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
              "Mandatory counter-currency-buy-order-amount value missing in strategy.xml config.");
    }
    LOG.info(() -> "<counter-currency-buy-order-amount> from config is: "
            + counterCurrencyBuyOrderAmountFromConfigAsString);

    // Will fail fast if value is not a number
    return new BigDecimal(counterCurrencyBuyOrderAmountFromConfigAsString);
  }

  private BigDecimal retrieveMinimumPercentageGainAmount(StrategyConfig config) {

    // Get min % gain...
    final String minimumPercentageGainFromConfigAsString =
            config.getConfigItem("minimum-percentage-gain");
    if (minimumPercentageGainFromConfigAsString == null) {
      // game over
      throw new IllegalArgumentException(
              "Mandatory minimum-percentage-gain value missing in strategy.xml config.");
    }
    LOG.info(() -> "<minimum-percentage-gain> from config is: "
            + minimumPercentageGainFromConfigAsString);

    // Will fail fast if value is not a number
    final BigDecimal minimumPercentageGainFromConfig =
            new BigDecimal(minimumPercentageGainFromConfigAsString);
    return minimumPercentageGainFromConfig
            .divide(new BigDecimal(100), 8, RoundingMode.HALF_UP);
  }
}
