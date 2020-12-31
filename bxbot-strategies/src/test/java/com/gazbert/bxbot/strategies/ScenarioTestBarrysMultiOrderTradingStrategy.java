package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategies.integration.SimulatedTradingApi;
import com.gazbert.bxbot.strategies.integration.StubMarket;
import com.gazbert.bxbot.strategies.integration.StubTransactionsRepository;
import com.gazbert.bxbot.strategies.integration.scenarios.Scenario;
import com.gazbert.bxbot.strategies.integration.scenarios.ScenarioEnum;
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
import org.junit.Test;

/**
 * Tests the performance of the multi-order Scalping Strategy.
 */
public class ScenarioTestBarrysMultiOrderTradingStrategy {

  private final Market market = new StubMarket();
  private final IStrategyConfigItems config = new BarrysMultiOrderStrategyConfigItems();
  private final TransactionsRepository transactionRepo = new StubTransactionsRepository();


  @Test
  public void randomWalkTest() throws Exception {

    Map<Scenario, Double> expResults = Map.of(
            ScenarioEnum.LINEAR_INCREASING, 1232.9994577249772,
            ScenarioEnum.VOLATILE_INCREASING, 1588.744577249768,
            ScenarioEnum.EXPONENTIAL_INCREASING, 2878.638434118917,
            ScenarioEnum.FLAT, 1067.9989154499538,
            ScenarioEnum.RANDOM_WALK, 1679.9891544995364,
            ScenarioEnum.EXPONENTIAL_DECREASING, 508.031577171763,
            ScenarioEnum.VOLATILE_DECREASING, 1140.9945772497686,
            ScenarioEnum.LINEAR_DECREASING, 834.9994577249765,
            ScenarioEnum.EXPONENTIAL_INCREASE_WITH_CRASH, 558.263138875568,
            ScenarioEnum.EXPONENTIAL_INCREASING_WITH_CRASHES, 1509.5463802500572
    );

    verifySimulationResults(expResults);
  }

  private void verifySimulationResults(Map<Scenario, Double> expectedResults) throws Exception {
    List<Scenario> scenarios =
            expectedResults.keySet().stream().sorted().collect(Collectors.toList());

    Map<String, Double> expectedResultValues = new LinkedHashMap<>();
    Map<String, Double> actualResultValues = new LinkedHashMap<>();

    BarrysMultiOrderTradingStrategy strategy = new BarrysMultiOrderTradingStrategy();

    for (Scenario scenario : scenarios) {
      expectedResultValues.put(scenario.getName(), expectedResults.get(scenario));
      double actualResultValue = runSimulation(strategy, scenario);
      actualResultValues.put(scenario.getName(), actualResultValue);
    }

    Assert.assertEquals(expectedResultValues.toString(), actualResultValues.toString());
  }

  private void verifySimulationResult(Scenario scenario, double expResult) throws Exception {
    BarrysMultiOrderTradingStrategy strategy = new BarrysMultiOrderTradingStrategy();
    double resultValue = runSimulation(strategy, scenario);
    Assert.assertEquals(expResult, resultValue, 0.01);
  }

  private double runSimulation(BarrysMultiOrderTradingStrategy strategy, Scenario scenario)
          throws Exception {

    SimulatedTradingApi tradingApi =
            new SimulatedTradingApi("simulated bitstamp", scenario.getSeriesData());
    TradingContext context = new TradingContext(tradingApi, market);

    strategy.init(context, config, transactionRepo);

    for (int i = 0; i < tradingApi.getNumSimulatedCycles(); i++) {
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
    return dollars + btcValue;
  }
}
