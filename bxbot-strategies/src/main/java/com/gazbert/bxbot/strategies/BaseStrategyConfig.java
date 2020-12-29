package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseStrategyConfig {

  protected static final Logger LOG = LogManager.getLogger();

  protected String strategyId;

  public String getStrategyId() {
    return strategyId;
  }

  protected String getConfigValueAsString(String property, IStrategyConfigItems config) {

    final String valueAsString =
            config.getConfigItem(property);

    if (valueAsString == null) {
      throw new IllegalArgumentException(
              "Mandatory " + property + " value missing in strategy.xml config.");
    }
    LOG.info(() -> "<" + property + "> from config is: "
            + valueAsString);

    return valueAsString;
  }

}
