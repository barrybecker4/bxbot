/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 gazbert
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.core.config.strategy;

import com.gazbert.bxbot.core.config.market.MarketImpl;
import com.gazbert.bxbot.domain.market.MarketConfig;
import com.gazbert.bxbot.domain.strategy.StrategyConfig;
import com.gazbert.bxbot.exchange.api.ExchangeAdapter;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.Market;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Util class that loads and initialises the Trading Strategies to execute.
 *
 * @author gazbert
 */
@Component
public class TradingStrategiesBuilder {

  private static final Logger LOG = LogManager.getLogger();
  private TradingStrategyFactory tradingStrategyFactory;

  // Set logic only as crude mechanism for checking for duplicate Markets.
  private final Set<Market> loadedMarkets = new HashSet<>();

  @Autowired
  public void setTradingStrategyFactory(TradingStrategyFactory tradingStrategyFactory) {
    this.tradingStrategyFactory = tradingStrategyFactory;
  }

  /** Builds the Trading Strategy execution list. */
  public List<TradingStrategy> buildStrategies(
      List<StrategyConfig> strategies,
      List<MarketConfig> markets,
      ExchangeAdapter exchangeAdapter) {

    final List<TradingStrategy> tradingStrategiesToExecute = new ArrayList<>();
    final Map<String, StrategyConfig> tradingStrategyConfigs =
            getStringStrategyConfigMap(strategies);

    // Load em up and create the Strategies
    for (final MarketConfig market : markets) {
      TradingStrategy strategy = loadStrategy(exchangeAdapter, tradingStrategyConfigs, market);
      if (strategy != null) {
        tradingStrategiesToExecute.add(strategy);
      }
    }
    return tradingStrategiesToExecute;
  }

  /** Load a single strategy given the exchange adapter and market. */
  private TradingStrategy loadStrategy(ExchangeAdapter exchangeAdapter,
       Map<String, StrategyConfig> tradingStrategyConfigs, MarketConfig market) {
    final Market tradingMarket = getTradingMarket(market);
    if (tradingMarket == null) {
      return null;
    }

    // Get the strategy to use for this Market
    final String strategyToUse = market.getTradingStrategyId();
    LOG.info(() -> "Market Trading Strategy Id to use: " + strategyToUse);

    if (tradingStrategyConfigs.containsKey(strategyToUse)) {
      final StrategyConfig tradingStrategy = tradingStrategyConfigs.get(strategyToUse);
      final StrategyConfigItems tradingStrategyConfig =
              new StrategyConfigItems(tradingStrategy.getId());
      final Map<String, String> configItems = tradingStrategy.getConfigItems();
      if (configItems != null && !configItems.isEmpty()) {
        tradingStrategyConfig.setItems(configItems);
      } else {
        LOG.info(
            () ->
                "No (optional) configuration has been set for Trading Strategy: "
                    + strategyToUse);
      }
      LOG.info(() -> "StrategyConfigImpl (optional): " + tradingStrategyConfig);

      /*
       * Load the Trading Strategy impl, instantiate it, set its config, and store in the
       * Trading Strategy execution list.
       */
      final TradingStrategy strategyImpl =
          tradingStrategyFactory.createTradingStrategy(tradingStrategy);
      strategyImpl.init(exchangeAdapter, tradingMarket, tradingStrategyConfig);

      LOG.info(
          () ->
              "Initialized trading strategy successfully. Name: ["
                  + tradingStrategy.getName()
                  + "] Class: "
                  + tradingStrategy.getClassName());

      return strategyImpl;
    } else {

      // Game over. Config integrity blown - we can't find strategy.
      final String errorMsg =
          "Failed to find matching Strategy for Market "
              + market
              + " - The Strategy "
              + "["
              + strategyToUse
              + "] cannot be found in the "
              + " Strategy Descriptions map: "
              + tradingStrategyConfigs;
      LOG.error(() -> errorMsg);
      throw new IllegalArgumentException(errorMsg);
    }
  }

  private Market getTradingMarket(MarketConfig market) {
    final String marketName = market.getName();
    if (!market.isEnabled()) {
      LOG.info(() -> marketName
              + " market is NOT enabled for trading - skipping to next market...");
      return null;
    }

    final Market tradingMarket =
        new MarketImpl(
            marketName, market.getId(), market.getBaseCurrency(), market.getCounterCurrency());
    final boolean wasAdded = loadedMarkets.add(tradingMarket);
    if (!wasAdded) {
      final String errorMsg = "Found duplicate Market! Market details: " + market;
      LOG.fatal(() -> errorMsg);
      throw new IllegalArgumentException(errorMsg);
    } else {
      LOG.info(() ->
          "Registered Market with Trading Engine: Id="
          + market.getId() + ", Name=" + marketName);
    }
    return tradingMarket;
  }

  private Map<String, StrategyConfig> getStringStrategyConfigMap(List<StrategyConfig> strategies) {
    // Register the strategies
    final Map<String, StrategyConfig> tradingStrategyConfigs = new HashMap<>();
    for (final StrategyConfig strategy : strategies) {
      tradingStrategyConfigs.put(strategy.getId(), strategy);
      LOG.info(() -> "Registered Trading Strategy with Trading Engine: Id=" + strategy.getId());
    }
    return tradingStrategyConfigs;
  }
}
