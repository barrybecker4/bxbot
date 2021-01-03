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

    // Barry's multi-order strategy
    expResults.put(ScenarioEnum.LINEAR_INCREASING, 1246.6687449900676);
    expResults.put(ScenarioEnum.VOLATILE_INCREASING, 1676.0152523952222);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING, 3777.0709648135107);
    expResults.put(ScenarioEnum.FLAT, 1106.8671234190554);
    expResults.put(ScenarioEnum.RANDOM_WALK, 1782.468174054819);
    expResults.put(ScenarioEnum.EXPONENTIAL_DECREASING, 706.0117655649542);
    expResults.put(ScenarioEnum.VOLATILE_DECREASING, 1140.9945772497686);
    expResults.put(ScenarioEnum.LINEAR_DECREASING, 867.5457903465663);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASE_WITH_CRASH, 607.4068058060816);
    expResults.put(ScenarioEnum.EXPONENTIAL_INCREASING_WITH_CRASHES, 1608.6149046197206);
    expResults.put(ScenarioEnum.HISTORICAL_DATA, 1185.3989547175693);

    verifySimulationResults(expResults);
  }

}
