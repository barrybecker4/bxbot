package com.gazbert.bxbot.domain.transaction;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;
import static org.junit.Assert.assertEquals;
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
  }

  @Test
  public void testToStringWorksAsExpected() {
    final TransactionEntry entry = new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.2, 345.);
    entry.setTimestamp(new Date(1607872141600L));

    assertEquals("TransactionEntry{id=null, orderId=42, "
                    + "type=BUY, status=SENT, market=Jack Bauer, amount=0.2, "
                    + "price=345.0, total=69.0, date=2020-12-13 07:09:01.600}",
            entry.toString());
  }
}
