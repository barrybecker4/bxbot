package com.gazbert.bxbot.strategies.integration.scenarios;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Some different cases to simulate.
 * Each series represents bitcoin prices over time.
 * There is no scalping algorithm that will work for all cases.
 * The idea is that we want to understand how each algorithm performs for each of these cases.
 * All the scenarios will start with BTC at a price of 25000.
 */
public class ExampleScenarios {

  static final List<ScenarioEnum> SCENARIOS_LIST = Arrays.asList(ScenarioEnum.values());

  /**
   * Print the scenarios in redable way.
   */
  public static void printScenarios() {
    String result = SCENARIOS_LIST.stream()
            .map(scenario -> scenario.getName()
                    + " = \n" + Arrays.toString(scenario.getSeriesData()))
            .collect(Collectors.joining(", "));

    System.out.println(result);
  }

  /**
   * Print for CSV file to import into google sheet and make graphs.
   */
  public static void printScenariosAsCsv() {
    int numValues = ScenarioEnum.FLAT.getSeriesData().length;
    for (ScenarioEnum scenario : SCENARIOS_LIST) {
      System.out.println(scenario.getName() + ",");
    }

    String headerRow = SCENARIOS_LIST.stream()
            .map(Scenario::getName)
            .collect(Collectors.joining(","));
    System.out.println("index," + headerRow);

    for (int i = 0; i < numValues; i++) {
      final int idx = i;
      String row = SCENARIOS_LIST.stream()
              .map(scenario -> Double.toString(scenario.getSeriesData()[idx]))
              .collect(Collectors.joining(","));
      System.out.println(idx + "," + row);
    }
  }

  public static void main(String[] args) {
    printScenariosAsCsv();
  }
}
