package com.gazbert.bxbot.domain.transaction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestTransactionEntry {


  @Test
  public void testInitialisationWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry(2L,"foo", "bar", 123.3);
    assertEquals(2L, entry.getId().longValue());
    assertEquals("foo", entry.getType());
    assertEquals("bar", entry.getMarket());
    assertEquals(123.3, entry.getAmount(), 0.00001);
  }

  @Test
  public void testSettersWorkAsExpected() {
    final TransactionEntry entry = new TransactionEntry(3L, "a", "b", 34.3);

    entry.setId(5L);
    assertEquals(5L, entry.getId().longValue());

    entry.setType("bar");
    assertEquals("bar", entry.getType());

    entry.setMarket("foo");
    assertEquals("foo", entry.getMarket());

    entry.setAmount(0.1);
    assertEquals(0.1, entry.getAmount(), 0.000001);
  }

  @Test
  public void testToStringWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry(3L, "myType", "myMarket", 3.4);

    assertEquals("TransactionEntry{id=3, type=myType, market=myMarket, amount=3.4}",
            entry.toString());
  }
}
