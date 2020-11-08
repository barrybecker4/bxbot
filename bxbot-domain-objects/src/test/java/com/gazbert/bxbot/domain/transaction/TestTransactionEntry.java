package com.gazbert.bxbot.domain.transaction;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestTransactionEntry {


  @Test
  public void testInitialisationWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("foo", "bar");
    assertEquals("foo", entry.getId());
    assertEquals("bar", entry.getType());
  }

  @Test
  public void testSettersWorkAsExpected() {
    final TransactionEntry entry = new TransactionEntry("a", "b");

    entry.setId("foo");
    assertEquals("foo", entry.getId());

    entry.setType("bar");
    assertEquals("bar", entry.getType());
  }

  @Test
  public void testToStringWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("myId", "myType");

    assertEquals("TransactionEntry{id=myId, type=myType}",
            entry.toString());
  }
}
