package com.gazbert.bxbot.strategies.integration.scenarios;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class ExponentialSeriesGenerator extends SeriesGenerator {

  private final double power;
  private final double volatility;
  private final Random rand;
  // positions of crashes in terms of percent of elapsed time
  private final List<Double> crashPoints;
  private final double degreeOfCrashes;

  /**
   * Generate exponential, random series with specified slope.
   *
   * @param power The scale factor, C, on the exponent in e^Cx.
   *              Values for C should be in range [-.1, -0) or (0,  0.1]
   * @param volatility a measure of how much the series jumps around. [0, 1]
   *      If 0, then a perfectly straight line. If 1 then every point will have a
   *      random number between [-initialValue/2, initialValue/2] added to it.
   * @param numCrashes the number of random crashes to occur. If just one, it happens at end
   * @param degreeOfCrashes determines how extreme the crashes are. A number between 0 and 1,
   *                        with 0 being the most extreme (goes all the way to 0)
   */
  public ExponentialSeriesGenerator(double initialValue, double power, double volatility,
                                    int numCrashes, double degreeOfCrashes) {
    super(initialValue);
    this.power = power;
    assert (volatility >= 0 && volatility <= 1);
    this.volatility = volatility;
    this.rand = new Random(0);
    this.degreeOfCrashes = degreeOfCrashes;

    if (numCrashes == 0) {
      crashPoints = Collections.singletonList(1.0);
    } else if (numCrashes == 1) {
      crashPoints = Arrays.asList(0.94, 1.0);
    } else {
      crashPoints = IntStream.range(0, numCrashes)
              .mapToDouble(i -> rand.nextDouble())
              .sorted().boxed().collect(Collectors.toList());
      crashPoints.add(1.0);
    }
  }

  /**
   * Generate exponential, random series with specified slope.
   *
   * @param power The scale factor, C, on the exponent in e^Cx.
   *      Values for C should be in range [-.1, -0) or (0,  0.1]
   * @param volatility a measure of how much the series jumps around. [0, 1]
   *      If 0, then a perfectly straight line. If 1 then every point will have a
   *      random number between [-initialValue/2, initialValue/2] added to it.
   */
  public ExponentialSeriesGenerator(double initialValue, double power, double volatility) {
    this(initialValue, power, volatility, 0, 0);
  }

  @Override
  double[] generateData(int numPoints) {

    double[] series = new double[numPoints];
    series[0] = initialValue;
    double startValue = initialValue;
    int crashCounter = 0;
    int x = 0;

    for (int i = 1; i < numPoints; i++) {
      double r = 1 + rand.nextDouble() * volatility - volatility / 2.0;
      series[i] = startValue * r * Math.pow(2, power * x);
      x++;

      double currentPct = (double) i / numPoints;
      if (currentPct >= crashPoints.get(crashCounter)) {
        // do a crash
        series[i] = degreeOfCrashes * (0.5 + rand.nextDouble() / 2) * initialValue;
        startValue = series[i];
        x = 1;
        crashCounter++;
      }
    }
    return series;
  }
}
