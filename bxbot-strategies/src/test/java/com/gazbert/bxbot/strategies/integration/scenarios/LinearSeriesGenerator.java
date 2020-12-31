package com.gazbert.bxbot.strategies.integration.scenarios;

import java.util.Random;

public class LinearSeriesGenerator extends SeriesGenerator {

  private final double slope;
  private final double volatility;
  private final Random rand;

  /**
   * Generate linear, random series with specified slope.
   *
   * @param slope general slope of the curve. 1 is sloping up at 45 degree angle. 0 is flat.
   * @param volatility a measure of how much the series jumps around. [0, 1]
   *      If 0, then a perfectly straight line. If 1 then every point will have a
   *      random number between [-initialValue/2, initialValue/2] added to it.
   */
  public LinearSeriesGenerator(double initialValue, double slope, double volatility) {
    super(initialValue);
    this.slope = slope;
    assert (volatility >= 0 && volatility <= 1);
    this.volatility = volatility;
    this.rand = new Random(4);
  }

  @Override
  double[] generateData(int numPoints) {

    double[] series = new double[numPoints];
    series[0] = initialValue;

    double randomness = volatility * initialValue;
    double slopeIncrement = initialValue * slope / numPoints;

    for (int i = 1; i < numPoints; i++) {
      double r = rand.nextDouble() * randomness - randomness / 2.0;
      series[i] = series[i - 1] + r + slopeIncrement;
      if (series[i] <= 0) {
        series[i] = -series[i];
      }
    }
    return series;
  }
}
