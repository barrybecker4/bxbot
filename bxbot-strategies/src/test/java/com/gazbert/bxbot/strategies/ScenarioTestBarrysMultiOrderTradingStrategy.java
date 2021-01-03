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
public class ScenarioTestBarrysMultiOrderTradingStrategy extends ScenarioTestBase {

  protected IStrategyConfigItems createStrategyConfigItems() {
    return new BarrysMultiOrderStrategyConfigItems();
  }

  protected AbstractTradingStrategy createTradingStrategy() {
    return new BarrysMultiOrderTradingStrategy();
  }

  @Test
  public void testAllScenariosTest() throws Exception {

    Map<Scenario, Double> expResults = new LinkedHashMap<>();
    expResults.put(ScenarioEnum.LINEAR_INCREASING, 1152.8866722375658);
    expResults.put(ScenarioEnum.VOLATILE_INCREASING, 1606.9151172355687);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING, 2747.879627523949);
    expResults.put(ScenarioEnum.FLAT, 1069.998715585135);
    expResults.put(ScenarioEnum.RANDOM_WALK, 1679.9891544995364);
    expResults.put(ScenarioEnum.EXPONENTIAL_DECREASING, 708.0115790349938);
    expResults.put(ScenarioEnum.VOLATILE_DECREASING, 1140.9945772497686);
    expResults.put(ScenarioEnum.LINEAR_DECREASING, 834.9994577249765);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASE_WITH_CRASH, 667.0896176429866);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING_WITH_CRASHES, 1509.5463802500572);
    expResults.put(ScenarioEnum.HISTORICAL_DATA, 1047.20179688);

    verifySimulationResults(expResults);
  }

}
