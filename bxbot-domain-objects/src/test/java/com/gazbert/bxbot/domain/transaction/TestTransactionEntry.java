package com.gazbert.bxbot.domain.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TestTransactionEntry {


  @Test
  public void testInitialisationWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("foo", "bar", 123.3);
    assertNull(entry.getId());
    assertEquals("foo", entry.getType());
    assertEquals("bar", entry.getMarket());
    assertEquals(123.3, entry.getAmount(), 0.00001);
  }

  @Test
  public void testToStringWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("myType", "myMarket", 3.4);

    assertEquals("TransactionEntry{id=null, type=myType, market=myMarket, amount=3.4}",
            entry.toString());
  }
}
