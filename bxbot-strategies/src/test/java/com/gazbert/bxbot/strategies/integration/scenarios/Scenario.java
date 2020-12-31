package com.gazbert.bxbot.strategies.integration.scenarios;

public interface Scenario {

  /**
   * Name of the scenario.
   *
   * @return name of the scenario
   */
  String getName();

  /**
   * Scenario data.
   *
   * @return the sequence of points representing BTC prices
   */
  double[] getSeriesData();

}
