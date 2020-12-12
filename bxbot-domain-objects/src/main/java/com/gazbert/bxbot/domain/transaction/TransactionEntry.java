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
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Domain object representing a transaction made by a strategy.
 *
 *<p>
 *  Fields to add:
 *   timestamp
 *   counter currency
 *   base currency
 *</p>
 *
 * @author Barry Becker
 */
@Entity
@Table(name = "TRANSACTIONS")
public class TransactionEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  private Long id;

  // BUY or SELL
  private String type;

  // the market that the transaction occurred on
  private String market;

  private Double amount;

  /** required no arg constructor. */
  public TransactionEntry() {
  }

  /** a transaction. */
  public TransactionEntry(String type, String market, Double amount) {
    this.type = type;
    this.market = market;
    this.amount = amount;
  }

  public Long getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public String getMarket() {
    return market;
  }

  public Double getAmount() {
    return amount;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", id)
      .add("type", type)
      .add("market", market)
      .add("amount", amount)
      .toString();
  }
}
