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
 * Should try comparing to other strategies like
 *  - buy and hold
 *  - Bill's rebalancing strategy
 */
public class ScenarioTestBarrysMultiOrderTradingStrategy {

  private final Market market = new StubMarket();
  private final IStrategyConfigItems config = new BarrysMultiOrderStrategyConfigItems();
  private final TransactionsRepository transactionRepo = new StubTransactionsRepository();


  @Test
  public void randomWalkTest() throws Exception {


    Map<Scenario, Double> expResults = new LinkedHashMap<>();
    expResults.put(ScenarioEnum.LINEAR_INCREASING, 1152.8866722375658);
    expResults.put(ScenarioEnum.VOLATILE_INCREASING, 1552.0820023504614);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING, 2741.8802282879005);
    expResults.put(ScenarioEnum.FLAT, 1069.998715585135);
    expResults.put(ScenarioEnum.RANDOM_WALK, 1679.9891544995364);
    expResults.put(ScenarioEnum.EXPONENTIAL_DECREASING, 644.770856855927);
    expResults.put(ScenarioEnum.VOLATILE_DECREASING, 1140.9945772497686);
    expResults.put(ScenarioEnum.LINEAR_DECREASING, 834.9994577249765);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASE_WITH_CRASH, 659.970662817741);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING_WITH_CRASHES, 1509.5463802500572);
    expResults.put(ScenarioEnum.HISTORICAL_DATA, 1047.20179688);


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

    for (int i = 0; i < tradingApi.getNumSimulatedCycles() - 1; i++) {
      tradingApi.advanceToNextTradingCyle();
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
    //System.out.println("amount of $=" + dollars + " amount of btc = " + btcValue);
    return dollars + btcValue;
  }
}
