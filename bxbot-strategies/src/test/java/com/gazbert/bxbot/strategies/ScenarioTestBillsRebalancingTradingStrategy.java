package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategies.integration.scenarios.Scenario;
import com.gazbert.bxbot.strategies.integration.scenarios.ScenarioEnum;
import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/**
 * Tests the performance of the multi-order Scalping Strategy.
 * Should try comparing to other strategies like
 *  - buy and hold
 *  - Bill's rebalancing strategy
 */
public class ScenarioTestBillsRebalancingTradingStrategy extends ScenarioTestBase {

  protected IStrategyConfigItems createStrategyConfigItems() {
    return new BillsRebalancingStrategyConfigItems();
  }

  protected AbstractTradingStrategy createTradingStrategy() {
    return new BillsRebalancingTradingStrategy();
  }

  @Test
  public void testAllScenariosTest() throws Exception {

    Map<Scenario, Double> expResults = new LinkedHashMap<>();
    expResults.put(ScenarioEnum.LINEAR_INCREASING, 1429.0203698769033);
    expResults.put(ScenarioEnum.VOLATILE_INCREASING, 2021.7335350394198);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING, 4500.905912653756);
    expResults.put(ScenarioEnum.FLAT, 1118.444951577584);
    expResults.put(ScenarioEnum.RANDOM_WALK, 2120.245032765944);
    expResults.put(ScenarioEnum.EXPONENTIAL_DECREASING, 77.63225380742851);
    expResults.put(ScenarioEnum.VOLATILE_DECREASING, 1219.1793934840493);
    expResults.put(ScenarioEnum.LINEAR_DECREASING, 688.8435268038278);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASE_WITH_CRASH, 171.20597914873613);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING_WITH_CRASHES, 1942.3531632376228);
    expResults.put(ScenarioEnum.HISTORICAL_DATA, 1844.5837059273517);

    verifySimulationResults(expResults);
  }

}
