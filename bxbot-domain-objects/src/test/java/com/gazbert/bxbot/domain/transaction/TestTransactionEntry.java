package com.gazbert.bxbot.domain.transaction;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Date;
import org.junit.Test;

public class TestTransactionEntry {


  @Test
  public void testInitialisationWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.234, 345.01);

    assertNull(entry.getId());
    assertEquals("42", entry.getOrderId());
    assertEquals("BUY", entry.getType());
    assertEquals("SENT", entry.getStatus());
    assertEquals("Jack Bauer", entry.getMarket());
    assertEquals(0.234, entry.getAmount(), 0.001);
    assertEquals(345.01, entry.getPrice(), 0.001);
    assertNotNull(entry.getTimestamp());
  }

  @Test
  public void testToStringWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.2, 345.);
    entry.setTimestamp(new Date(1607872141600L));

    assertEquals("TransactionEntry{id=null, orderId=42, "
                    + "type=BUY, status=SENT, market=Jack Bauer, amount=0.2, "
                    + "price=345.0, total=69.0, timestamp=2020-12-13 07:09:01.600}",
            entry.toString());
  }

  @Test
  public void testEqualsWorksAsExpected() {
    final TransactionEntry entry1 = new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.2, 345.);
    entry1.setTimestamp(new Date(1607872141600L));
    final TransactionEntry entry2 = new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.2, 345.);
    entry2.setTimestamp(new Date(1607872141700L));

    final TransactionEntry entry3 = new TransactionEntry("43", "SELL", SENT,
            "Kim Bauer", 0.2, 345.);
    entry3.setTimestamp(new Date(1607872141800L));


    assertEquals(entry1, entry2);
    assertNotEquals(entry1, entry3);
  }

}
