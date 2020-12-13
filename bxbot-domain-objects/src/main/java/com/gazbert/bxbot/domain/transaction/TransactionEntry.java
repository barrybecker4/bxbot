/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.domain.transaction;

import com.google.common.base.MoreObjects;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


/**
 * Domain object representing a transaction made by a strategy.
 *
 * @author Barry Becker
 */
@Entity
@Table(name = "TRANSACTIONS")
public class TransactionEntry {

  public enum Status { SENT, FILLED }

  // There is a spotbugs multi-threaded warning if this is static
  private final DateFormat dateFormat =
          new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  // id of the order on the exchange
  private String orderId;

  // BUY or SELL
  private String type;

  // Status is either SENT or FILLED. Not all sent orders will necessarily fill.
  private String status;

  // the market that the transaction occurred on
  private String market;

  // the base currency to buy or sell (if the order gets filled).
  private Double amount;

  // the bid or ask price (depending on whether this order is buy or sell) in the counter currency.
  private Double price;

  // when this transaction was SENT or FILLED (with resolution of trading cylce)
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;


  /** required no arg constructor. */
  public TransactionEntry() {
  }

  /** a transaction. */
  public TransactionEntry(String orderId, String type, Status status,
                          String market, Double amount, Double price) {
    this.orderId = orderId;
    this.type = type;
    this.status = status.toString();
    this.market = market;
    this.price = price;
    this.amount = amount;
    this.timestamp = new Date();
  }

  public TransactionEntry(String orderId, String type, Status status,
                          String market, BigDecimal amount, BigDecimal price) {
    this(orderId, type, status, market, amount.doubleValue(), price.doubleValue());
  }

  public Long getId() {
    return id;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getType() {
    return type;
  }

  public String getStatus() {
    return status;
  }

  public String getMarket() {
    return market;
  }

  public Double getAmount() {
    return amount;
  }

  public Double getPrice() {
    return price;
  }

  public Date getTimestamp() {
    return new Date(timestamp.getTime());
  }

  void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("orderId", orderId)
      .add("type", type)
      .add("status", status)
      .add("market", market)
      .add("amount", amount)
      .add("price", price)
      .add("total", amount * price)
      .add("date", dateFormat.format(timestamp))
      .toString();
  }
}
