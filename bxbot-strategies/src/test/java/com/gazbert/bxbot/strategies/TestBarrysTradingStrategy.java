/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Gareth Jon Lynch
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

package com.gazbert.bxbot.strategies;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.FILLED;
import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

/**
 * Tests the behaviour of the example Scalping Strategy.
 * Some fairly decent coverage here, but we've not tested all the exception handling conditions -
 * you will want to do a much more thorough job
 * than this with your own strategies :-)
 *
 * @author gazbert
 */
public class TestBarrysTradingStrategy {

  private static final String MARKET_ID = "btc_usd";
  private static final String MARKET_NAME = "BTC_USD";
  private static final String BASE_CURRENCY = "BTC";
  private static final String COUNTER_CURRENCY = "USD";

  private static final String CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT = "20"; // USD amount
  private static final String CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN = "2";
  private static final String ORDER_ID = "45345346";

  private TradingContext context;
  private TradingApi tradingApi;
  private Market market;
  private StrategyConfig config;
  private TransactionsRepository transactionRepo;

  private MarketOrderBook marketOrderBook;
  private MarketOrder marketBuyOrder;
  private MarketOrder marketSellOrder;

  private List<MarketOrder> marketBuyOrders;
  private List<MarketOrder> marketSellOrders;

  /** Each test will have the same up to the point of fetching the order book. */
  @Before
  public void setUpBeforeEachTest() throws Exception {
    context = createMock(TradingContext.class);
    tradingApi = createMock(TradingApi.class);
    market = createMock(Market.class);
    config = createMock(StrategyConfig.class);
    transactionRepo = createMock(TransactionsRepository.class);

    // setup market order book
    marketOrderBook = createMock(MarketOrderBook.class);
    marketBuyOrder = createMock(MarketOrder.class);
    marketBuyOrders = new ArrayList<>();
    marketBuyOrders.add(marketBuyOrder);
    marketSellOrders = new ArrayList<>();
    marketSellOrder = createMock(MarketOrder.class);
    marketSellOrders.add(marketSellOrder);

    // expect config to be loaded
    expect(config.getConfigItem("counter-currency-buy-order-amount"))
        .andReturn(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT);
    expect(config.getConfigItem("minimum-percentage-gain"))
        .andReturn(CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN);

    // expect Market name to be logged zero or more times.
    expect(context.getMarketName()).andReturn(MARKET_NAME).anyTimes();
    expect(tradingApi.getMarketOrders(MARKET_ID)).andReturn(marketOrderBook).anyTimes();
    expect(market.getName()).andReturn(MARKET_NAME).anyTimes();
    expect(market.getId()).andReturn(MARKET_ID).anyTimes();

    // expect market order book to be fetched
    expect(marketOrderBook.getBuyOrders()).andReturn(marketBuyOrders);
    expect(marketOrderBook.getSellOrders()).andReturn(marketSellOrders);
    expect(context.getBuyOrders()).andReturn(marketBuyOrders);
    expect(context.getSellOrders()).andReturn(marketSellOrders);
  }

  /*
   * Tests scenario when bot has just started, and the strategy is invoked for the first time.
   *
   * - Given the bot has just started
   * - When the strategy is first invoked
   * - Then a new buy order is sent to the exchange
   */
  @Test
  public void testStrategySendsInitialBuyOrderWhenItIsFirstCalled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to get amount of base currency to buy for given counter currency amount
    final BigDecimal lastTradePrice = new BigDecimal("1453.014"); //("1454.018");

    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    OrderState expBuyOrder =
            new OrderState(ORDER_ID, OrderType.BUY, bidSpotPrice, amountOfUnitsToBuy);

    expect(context.sendBuyOrder(amountOfUnitsToBuy, lastTradePrice)).andReturn(expBuyOrder);

    TransactionEntry expEntry = new TransactionEntry(ORDER_ID, OrderType.BUY.getStringValue(), SENT,
            MARKET_NAME, amountOfUnitsToBuy, bidSpotPrice);

    expect(transactionRepo.save(expEntry)).andReturn(expEntry);

    replay(context, config, transactionRepo, marketBuyOrder, marketSellOrder);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();
    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, transactionRepo, marketBuyOrder, marketSellOrder);
  }

  /*
   * Tests scenario when strategy has had its current buy order filled. We expect it to create a
   * new sell order.
   *
   * - Given the bot has had its current buy order filled
   * - When the strategy is invoked
   * - Then a new sell order is sent to the exchange
   */
  @Test
  public void testStrategySendsNewSellOrderToExchangeWhenCurrentBuyOrderFilled() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock the buy order state that was filled
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.BUY, lastOrderPrice, lastOrderAmount);

    TransactionEntry buyEntry = new TransactionEntry(
            ORDER_ID, OrderType.BUY.getStringValue(), FILLED,
            MARKET_NAME, lastOrderAmount, lastOrderPrice);
    expect(transactionRepo.save(buyEntry)).andReturn(buyEntry);

    // expect to send new sell order to exchange
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");
    final BigDecimal newAskPrice =
        lastOrderPrice
            .multiply(requiredProfitInPercent)
            .add(lastOrderPrice)
            .setScale(8, RoundingMode.HALF_UP);
    final String orderId = "4239407234";

    OrderState expSellOrder =
            new OrderState(ORDER_ID, OrderType.SELL, newAskPrice, lastOrderAmount);
    expect(context.sendSellOrder(lastOrderAmount, newAskPrice)).andReturn(expSellOrder);

    TransactionEntry sellEntry = new TransactionEntry(
            ORDER_ID, OrderType.SELL.getStringValue(), SENT,
            MARKET_NAME, lastOrderAmount, newAskPrice);
    expect(transactionRepo.save(sellEntry)).andReturn(sellEntry);

    replay(context, config, transactionRepo, marketBuyOrder, marketSellOrder, orderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, transactionRepo, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * Tests scenario when strategy's current buy order is still waiting to be filled. We expect
   * it to hold.
   *
   * - Given the bot has placed a buy order and it had not filled
   * - When the strategy is invoked
   * - Then the bot holds until the next trade cycle
   */
  @Test
  public void testStrategyHoldsWhenCurrentBuyOrderIsNotFilled() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(true);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.BUY, lastOrderPrice, lastOrderAmount);

    // expect to check if the buy order has filled
    final OpenOrder unfilledOrder = createMock(OpenOrder.class);
    final List<OpenOrder> openOrders = new ArrayList<>();
    openOrders.add(unfilledOrder); // still have open order

    replay(context, config, marketBuyOrder, marketSellOrder,
        orderState, unfilledOrder);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder,
        orderState, unfilledOrder);
  }

  /*
   * Tests scenario when strategy has had its current sell order filled. We expect it to create a
   * new buy order.
   *
   * - Given the bot has had its current sell order filled
   * - When the strategy is invoked
   * - Then a new buy order is sent to the exchange
   */
  @Test
  public void testStrategySendsNewBuyOrderToExchangeWhenCurrentSellOrderFilled() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object sellOrderState = createMockOrder(OrderType.SELL, lastOrderPrice, lastOrderAmount);

    final TransactionEntry sellEntry =
            new TransactionEntry(ORDER_ID, OrderType.SELL.getStringValue(), FILLED,
                    MARKET_NAME, lastOrderAmount, lastOrderPrice);
    expect(transactionRepo.save(sellEntry)).andReturn(sellEntry);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    final TransactionEntry buyEntry =
            new TransactionEntry(ORDER_ID, OrderType.BUY.getStringValue(), SENT,
                    MARKET_NAME, amountOfUnitsToBuy, bidSpotPrice);
    expect(transactionRepo.save(buyEntry)).andReturn(buyEntry);

    // expect to send new buy order to exchange
    OrderState expOrder =
            new OrderState(ORDER_ID, OrderType.BUY, bidSpotPrice, amountOfUnitsToBuy);
    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice)).andReturn(expOrder);

    replay(context, config, transactionRepo, marketBuyOrder, marketSellOrder, sellOrderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", sellOrderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, transactionRepo, marketBuyOrder, marketSellOrder, sellOrderState);
  }

  /*
   * Tests scenario when strategy's current sell order is still waiting to be filled. We expect
   * it to hold.
   *
   * - Given the bot has placed a sell order and it has not filled
   * - When the strategy is invoked,
   * - Then the bot holds until the next trade cycle
   */
  @Test
  public void testStrategyHoldsWhenCurrentSellOrderIsNotFilled() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    expect(context.isOrderOpen("4239407233")).andReturn(true);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    OrderState expOrderState =
            new OrderState("4239407233", OrderType.SELL, lastOrderPrice, lastOrderAmount);

    replay(context, config, marketBuyOrder, marketSellOrder);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", expOrderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder);
  }

  // ------------------------------------------------------------------------
  // Timeout exception handling tests
  // ------------------------------------------------------------------------

  /*
   * When attempting to send the initial buy order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent initial buy order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingInitialBuyOrder() throws Exception {
    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // expect to send initial buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    expect(market.getId()).andReturn(MARKET_ID);
    expect(tradingApi.createOrder(MARKET_ID, OrderType.BUY, amountOfUnitsToBuy, bidSpotPrice));

    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice))
            .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(context, config, marketBuyOrder, marketSellOrder);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();
    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder);
  }

  /*
   * When attempting to send a buy order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent a buy order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingBuyOrder() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.SELL, lastOrderPrice, lastOrderAmount);

    // expect to send new buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice))
           .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(context, config, marketBuyOrder, marketSellOrder, orderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * When attempting to send a sell order to the exchange, a timeout exception is received.
   * We expect the strategy to swallow it and exit until the next trade cycle.
   *
   * - Given the strategy has just sent a sell order
   * - When a timeout exception is caught
   * - Then the strategy returns without error
   */
  @Test
  public void testStrategyHandlesTimeoutExceptionWhenPlacingSellOrder() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing buy order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.BUY, lastOrderPrice, lastOrderAmount);

    // expect to send new sell order to exchange and receive timeout exception
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");
    final BigDecimal newAskPrice =
        lastOrderPrice
            .multiply(requiredProfitInPercent)
            .add(lastOrderPrice)
            .setScale(8, RoundingMode.HALF_UP);

    expect(context.sendSellOrder(lastOrderAmount, newAskPrice))
            .andThrow(new ExchangeNetworkException("Timeout waiting for exchange!"));

    replay(context, config, marketBuyOrder, marketSellOrder, orderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder, orderState);
  }

  // ------------------------------------------------------------------------
  // Trading API exception handling tests
  // ------------------------------------------------------------------------

  /*
   * When attempting to send the initial buy order to the exchange, a Trading API exception is
   * received. We expect the strategy to wrap it in a Strategy exception and throw it to the
   * Trading Engine.
   *
   * - Given the strategy has just sent initial buy order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingInitialBuyOrder() throws Exception {

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice))
            .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(context, config, marketBuyOrder, marketSellOrder);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();
    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder);
  }

  /*
   * When attempting to send a buy order to the exchange, a Trading API exception is received.
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   *
   * - Given the strategy has just sent a buy order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingBuyOrder() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    // mock an existing sell order state
    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.SELL, lastOrderPrice, lastOrderAmount);

    // expect to send new buy order to exchange and receive timeout exception
    final BigDecimal amountOfUnitsToBuy = new BigDecimal("1333.33333333");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice))
            .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(context, config, marketBuyOrder, marketSellOrder, orderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing sell order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder, orderState);
  }

  /*
   * When attempting to send a sell order to the exchange, a Trading API exception is received.
   * We expect the strategy to wrap it in a Strategy exception and throw it to the Trading Engine.
   *
   * - Given the strategy has just sent a sell order
   * - When a Trading API exception is caught
   * - Then the strategy throws a Strategy exception
   */
  @Test(expected = StrategyException.class)
  public void testStrategyHandlesTradingApiExceptionWhenPlacingSellOrder() throws Exception {

    expect(context.isOrderOpen(ORDER_ID)).andReturn(false);

    // expect to get current bid and ask spot prices
    final BigDecimal bidSpotPrice = new BigDecimal("1453.014");
    expect(marketBuyOrders.get(0).getPrice()).andReturn(bidSpotPrice);
    final BigDecimal askSpotPrice = new BigDecimal("1455.016");
    expect(marketSellOrders.get(0).getPrice()).andReturn(askSpotPrice);

    final BigDecimal lastOrderAmount = new BigDecimal("35");
    final BigDecimal lastOrderPrice = new BigDecimal("1454.018");
    final Object orderState = createMockOrder(OrderType.SELL, lastOrderPrice, lastOrderAmount);

    final TransactionEntry sellEntry =
            new TransactionEntry(ORDER_ID, OrderType.SELL.getStringValue(), FILLED,
                    MARKET_NAME, lastOrderAmount, bidSpotPrice);
    expect(transactionRepo.save(sellEntry)).andReturn(sellEntry);

    // expect to send new sell order to exchange and receive timeout exception
    final BigDecimal requiredProfitInPercent = new BigDecimal("0.02");

    final BigDecimal amountOfUnitsToBuy = new BigDecimal("0.01375499");
    expect(context.getAmountOfBaseCurrencyToBuy(
            new BigDecimal(CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT)))
            .andReturn(amountOfUnitsToBuy);

    expect(context.sendBuyOrder(amountOfUnitsToBuy, bidSpotPrice))
            .andThrow(new TradingApiException("Exchange returned a 500 status code!"));

    replay(context, config, marketBuyOrder, marketSellOrder, orderState);

    final BarrysTradingStrategy strategy = new BarrysTradingStrategy();

    // inject the existing buy order
    Whitebox.setInternalState(strategy, "lastOrder", orderState);

    strategy.init(context, config, transactionRepo);
    strategy.execute();

    verify(context, config, marketBuyOrder, marketSellOrder, orderState);
  }

  // mock existing buy or sell order state
  private Object createMockOrder(OrderType type,
                                 BigDecimal lastOrderPrice, BigDecimal lastOrderAmount)
          throws ClassNotFoundException {

    final Object orderState = createMock(OrderState.class);
    Whitebox.setInternalState(orderState, "id", ORDER_ID);
    Whitebox.setInternalState(orderState, "type", type);
    Whitebox.setInternalState(orderState, "price", lastOrderPrice);
    Whitebox.setInternalState(orderState, "amount", lastOrderAmount);
    return orderState;
  }
}
