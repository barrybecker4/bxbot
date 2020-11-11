package com.gazbert.bxbot.strategies;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import org.junit.Test;

public class TestPriceUtil {

  @Test
  public void testFormatSmallPrice() {

    assertEquals("0.0003211",
            PriceUtil.formatPrice(new BigDecimal("0.00032109876")));
    assertEquals("1.23456789",
            PriceUtil.formatPrice(new BigDecimal("1.234567890123")));
  }

  @Test
  public void testFormatLargePrice() {

    assertEquals("12345678.234567",
            PriceUtil.formatPrice(new BigDecimal("12345678.234567")));
    assertEquals("99987612345678.23456781",
            PriceUtil.formatPrice(new BigDecimal("99987612345678.2345678123456789")));
  }
}
