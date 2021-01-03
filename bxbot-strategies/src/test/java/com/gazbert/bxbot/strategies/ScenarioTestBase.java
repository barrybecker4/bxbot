package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategies.integration.SimulatedTradingApi;
import com.gazbert.bxbot.strategies.integration.StubMarket;
import com.gazbert.bxbot.strategies.integration.StubTransactionsRepository;
import com.gazbert.bxbot.strategies.integration.scenarios.Scenario;
import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Assert;

/**
 * Tests the performance of Strategies against a set of scenarios.
 */
abstract class ScenarioTestBase {

  protected final Market market = new StubMarket();
  protected final IStrategyConfigItems config = createStrategyConfigItems();
  private final TransactionsRepository transactionRepo = new StubTransactionsRepository();

  protected abstract IStrategyConfigItems createStrategyConfigItems();

  protected abstract AbstractTradingStrategy createTradingStrategy();

  protected void verifySimulationResults(Map<Scenario, Double> expectedResults) throws Exception {
    List<Scenario> scenarios =
            expectedResults.keySet().stream().sorted().collect(Collectors.toList());

    Map<String, Double> expectedResultValues = new LinkedHashMap<>();
    Map<String, Double> actualResultValues = new LinkedHashMap<>();

    AbstractTradingStrategy strategy = createTradingStrategy();

    for (Scenario scenario : scenarios) {
      expectedResultValues.put(scenario.getName(), expectedResults.get(scenario));
      double actualResultValue = runSimulation(strategy, scenario);
      actualResultValues.put(scenario.getName(), actualResultValue);
    }

    Assert.assertEquals(expectedResultValues.toString(), actualResultValues.toString());
  }

  private double runSimulation(AbstractTradingStrategy strategy, Scenario scenario)
          throws Exception {

    System.out.println("Now running simulation for scenario " + scenario.getName());

    SimulatedTradingApi tradingApi =
            new SimulatedTradingApi("simulated exchange", scenario.getSeriesData());
    TradingContext context = new TradingContext(tradingApi, market);

    strategy.init(context, config, transactionRepo);

    for (int i = 0; i < tradingApi.getNumSimulatedCycles() - 1; i++) {
      tradingApi.advanceToNextTradingCycle();
      strategy.execute();
    }

    return getResultValue(tradingApi);
  }

  private double getResultValue(TradingApi tradingApi)
          throws TradingApiException, ExchangeNetworkException {
    Map<String, BigDecimal> balances = tradingApi.getBalanceInfo().getBalancesAvailable();
    double latestPrice = tradingApi.getLatestMarketPrice(market.getId()).doubleValue();

    double dollars = balances.get("USD").doubleValue();
    double btcValue = balances.get("BTC").doubleValue() * latestPrice;
    System.out.println("amount of usd = $" + dollars + ";  amount of btc = $" + btcValue);
    return dollars + btcValue;
  }
}
