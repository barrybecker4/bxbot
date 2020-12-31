package com.gazbert.bxbot.strategies.integration.scenarios;

public abstract class SeriesGenerator {

  protected double initialValue;

  abstract double[] generateData(int numPoints);

  public SeriesGenerator(double initialValue) {
    this.initialValue = initialValue;
  }
}
