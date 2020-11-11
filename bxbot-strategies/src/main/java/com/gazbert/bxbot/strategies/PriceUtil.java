package com.gazbert.bxbot.strategies;

import java.math.BigDecimal;
import java.text.DecimalFormat;

public class PriceUtil {

  private static final String DECIMAL_FORMAT = "#.########";
  private static final DecimalFormat FORMATTER = new DecimalFormat(DECIMAL_FORMAT);


  public static String formatPrice(BigDecimal price) {
    return FORMATTER.format(price);
  }
}
