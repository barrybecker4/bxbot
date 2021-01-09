package com.gazbert.bxbot.strategies.integration.scenarios;

/**
 * Here are some ideas for cases.
 *
 * <pre>
 * Case 1 - linear increasing (done)
 * Case 2 - volatile, but trending up  (done)
 * Case 3 - exponentially increasing (e^x) - with randomnesss  (done)
 * Case 4 - mountain/inverted V.         - skip for now
 * Case 5 - flat  (done)
 * Case 6 - random walk  (done)
 * Case 7 - V-shaped - up then down       - skip for now
 * Case 8 - exponentially decreasing (e^-x) - with randomness  (done)
 * Case 9 - volatile, but trending down  (done)
 * Case 10 - linear decreasing  (done)
 * Case 11 - exponential increase, then big crash (what I think will happen)
 * Case 12 - many exponential increases followed by crashes
 * Case 13 - a day of historical BTC data sampled every minute
 * Case 14 - a week of historical BTC data sampled every hour
 * Case 15 - a different week of historical BTC data sampled every hour
 * Case 16 - 3 months of historical BTC data sampled every day
 * Case 17 - 2 years of historical BTC data sampled every week
 * </pre>
 */
public enum ScenarioEnum implements Scenario {

  LINEAR_INCREASING("linearlyIncreasing",
          new LinearSeriesGenerator(25000, 0.4, 0.02)) {
  },

  VOLATILE_INCREASING("volatileIncreasing",
          new LinearSeriesGenerator(25000, 0.5, 0.2)) {
  },

  EXPONENTIAL_INCREASING("exponentiallyIncreasing",
          new ExponentialSeriesGenerator(25000, 0.0115, 0.08)) {
  },

  FLAT("flat",
          new LinearSeriesGenerator(25000, 0.0, 0.04)) {
  },

  RANDOM_WALK("randomWalk",
          new LinearSeriesGenerator(25000, 0.0, 0.4)) {
  },

  EXPONENTIAL_DECREASING("exponentiallyDecreasing",
          new ExponentialSeriesGenerator(25000, -0.03, 0.06)) {
  },

  VOLATILE_DECREASING("volatileDecreasing",
          new LinearSeriesGenerator(25000, -0.4, 0.2)) {
  },

  LINEAR_DECREASING("linearlyIDecreasing",
          new LinearSeriesGenerator(25000, -0.4, 0.02)) {
  },

  EXPONENTIAL_INCREASE_WITH_CRASH("crashAfterExponentiallyIncrease",
         new ExponentialSeriesGenerator(25000, 0.0129, 0.08,
                 1, 0.2)) {
  },

  EXPONENTIAL_INCREASING_WITH_CRASHES("exponentiallyIncreaseWithCrashes",
          new ExponentialSeriesGenerator(25000, 0.04, 0.09,
                  4, 0.9)) {
  },

  HISTORICAL_DATA("historicalDataLastHalf2020", new RealDataGenerator(25000)) {
  };

  private static final int NUM_POINTS = 200;

  protected final String name;
  private final double[] seriesData;

  ScenarioEnum(String name, SeriesGenerator generator) {
    this.name = name;
    this.seriesData = generator.generateData(NUM_POINTS);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public final double[] getSeriesData() {
    return seriesData;
  }

  public int compareTo(Scenario s) {
    return this.name.compareTo(s.getName());
  }
}
