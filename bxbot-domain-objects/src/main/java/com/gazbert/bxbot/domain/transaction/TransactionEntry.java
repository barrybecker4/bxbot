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
import java.util.Objects;
import java.util.TimeZone;
import javax.persistence.Basic;
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

  public enum Status {
    SENT,
    FILLED
  }

  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
  // timestamps use California time
  private static final TimeZone TIME_ZONE = TimeZone.getTimeZone("PST");

  // added columns
  // ALTER TABLE TRANSACTIONS ADD value DOUBLE DEFAULT (price * amount) NOT NULL
  // ALTER TABLE TRANSACTIONS ADD strategy VARCHAR(50) DEFAULT 'barrys-strategy' NOT NULL
  // ALTER TABLE TRANSACTIONS ADD exchange VARCHAR(30) DEFAULT 'Bitstamp Exchange' NOT NULL


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

  // the value of the transaction in counter currence. i.e. price * amount
  private Double value;

  // the name of the strategy used to make the transaction
  private String strategy;

  // the name of the exchange API used (e.g. bitstamp, kraken, binance, etc)
  private String exchangeApi;

  // when this transaction was SENT or FILLED (with resolution of trading cycle)
  @Basic
  @Temporal(TemporalType.TIMESTAMP)
  private Date timestamp;


  /** required no arg constructor. */
  public TransactionEntry() {
  }

  /** a transaction. */
  public TransactionEntry(String orderId, String type, Status status,
                          String market, Double amount, Double price,
                          String strategy, String exchangeApi) {
    this.orderId = orderId;
    this.type = type;
    this.status = status.toString();
    this.market = market;
    this.price = price;
    this.amount = amount;
    this.value = price * amount;
    this.strategy = strategy;
    this.exchangeApi = exchangeApi;
    this.timestamp = new Date();
  }

  public TransactionEntry(String orderId, String type, Status status,
                          String market, BigDecimal amount, BigDecimal price,
                          String strategy, String exchangeApi) {
    this(orderId, type, status, market, amount.doubleValue(), price.doubleValue(),
            strategy, exchangeApi);
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

  public Double getValue() {
    return value;
  }

  public String getStrategy() {
    return strategy;
  }

  public String getExchangeApi() {
    return exchangeApi;
  }

  public Date getTimestamp() {
    return new Date(timestamp.getTime());
  }

  void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  @Override
  public String toString() {
    assert (type != null);
    assert (status != null);
    assert (amount != null);
    assert (price != null);
    assert (timestamp != null);

    DateFormat format = new SimpleDateFormat(DATE_FORMAT);
    format.setTimeZone(TIME_ZONE);

    return MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("orderId", orderId)
      .add("type", type)
      .add("status", status)
      .add("market", market)
      .add("amount", amount)
      .add("price", price)
      .add("value", value)
      .add("strategy", strategy)
      .add("exchangeApi", exchangeApi)
      .add("timestamp", format.format(timestamp))
      .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TransactionEntry that = (TransactionEntry) o;
    return orderId.equals(that.orderId)
            && type.equals(that.type)
            && status.equals(that.status)
            && market.equals(that.market)
            && amount.equals(that.amount)
            && price.equals(that.price)
            && strategy.equals(that.strategy)
            && exchangeApi.equals(that.exchangeApi);
  }

  @Override
  public int hashCode() {
    return Objects.hash(orderId, type, status, market, amount, price, strategy, exchangeApi);
  }
}
