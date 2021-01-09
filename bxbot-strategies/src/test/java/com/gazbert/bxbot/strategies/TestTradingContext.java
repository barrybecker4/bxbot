package com.gazbert.bxbot.strategies;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;


public class TestTradingContext {

  private static final String MARKET_ID = "btc_usd";
  private static final String ORDER_ID = "45345346";

  private TradingApi tradingApi;
  private Market market;
  private IStrategyConfigItems config;

  private MarketOrderBook marketOrderBook;
  private MarketOrder marketBuyOrder;
  private MarketOrder marketSellOrder;

  private List<MarketOrder> marketBuyOrders;
  private List<MarketOrder> marketSellOrders;

  /** Each test will have the same up to the point of fetching the order book. */
  @Before
  public void setUpBeforeEachTest() throws Exception {
    tradingApi = createMock(TradingApi.class);
    market = createMock(Market.class);
    config = createMock(IStrategyConfigItems.class);

    // setup market order book
    marketOrderBook = createMock(MarketOrderBook.class);
    marketBuyOrder = createMock(MarketOrder.class);
    marketBuyOrders = new ArrayList<>();
    marketBuyOrders.add(marketBuyOrder);
    marketSellOrders = new ArrayList<>();
    marketSellOrder = createMock(MarketOrder.class);
    marketSellOrders.add(marketSellOrder);

    // expect Market name to be logged zero or more times.
    expect(tradingApi.getMarketOrders(MARKET_ID)).andReturn(marketOrderBook).anyTimes();
    expect(market.getName()).andReturn("BTC_USD").anyTimes();
    expect(market.getId()).andReturn(MARKET_ID).anyTimes();
  }

  @Test
  public void testMarketName() {
    replay(market);

    final TradingContext context = new TradingContext(tradingApi, market);
    context.getMarketName();
    assertEquals(context.getMarketName(), "BTC_USD");

    verify(market);
  }

  /*
   * Tests buy orders.
   */
  @Test
  public void testGetBuyOrders() throws Exception {

    expect(marketOrderBook.getBuyOrders()).andReturn(marketBuyOrders);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final TradingContext context = new TradingContext(tradingApi, market);
    List<MarketOrder> orders = context.getBuyOrders();
    assertEquals(marketBuyOrders, orders);

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

  /*
   * Tests sell orders.
   */
  @Test
  public void testGetSellOrders() throws Exception {

    expect(marketOrderBook.getSellOrders()).andReturn(marketSellOrders);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final TradingContext context = new TradingContext(tradingApi, market);
    List<MarketOrder> orders = context.getSellOrders();
    assertEquals(marketSellOrders, orders);

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

  /*
   * Tests send sell order.
   */
  @Test
  public void testSendSellOrder() throws Exception {

    BigDecimal amountOfBaseCurrencyToSell = new BigDecimal("1.5");
    BigDecimal askPrice = new BigDecimal("123.45");
    expect(tradingApi.createOrder(MARKET_ID, OrderType.SELL, amountOfBaseCurrencyToSell, askPrice))
            .andReturn(ORDER_ID);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final TradingContext context = new TradingContext(tradingApi, market);

    OrderState orderState = new OrderState();
    orderState.id = ORDER_ID;
    orderState.type = OrderType.SELL;
    orderState.price = askPrice;
    orderState.amount = amountOfBaseCurrencyToSell;

    assertEquals(
            orderState.toString(),
            context.sendSellOrder(amountOfBaseCurrencyToSell, askPrice).toString()
    );

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }


  /*
   * Tests if order is open when there is one open
   */
  @Test
  public void testIsOrderOpenWhenOpen() throws Exception {

    OpenOrder order1 = createMock(OpenOrder.class);
    expect(order1.getId()).andReturn("123");
    OpenOrder order2 = createMock(OpenOrder.class);
    expect(order2.getId()).andReturn("234");

    List<OpenOrder> openOrders = Arrays.asList(order1, order2);

    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder,
            order1, order2);

    final TradingContext context = new TradingContext(tradingApi, market);
    assertTrue(context.isOrderOpen("234"));

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder,
            order1, order2);
  }

  /*
   * Tests if order is open when there is no open order with specified id
   */
  @Test
  public void testIsOrderOpenWhenNot() throws Exception {

    OpenOrder order1 = createMock(OpenOrder.class);
    expect(order1.getId()).andReturn("123");
    OpenOrder order2 = createMock(OpenOrder.class);
    expect(order2.getId()).andReturn("987");

    List<OpenOrder> openOrders = Arrays.asList(order1, order2);

    expect(tradingApi.getYourOpenOrders(MARKET_ID)).andReturn(openOrders);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder,
            order1, order2);

    final TradingContext context = new TradingContext(tradingApi, market);
    assertFalse(context.isOrderOpen("234"));

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder,
            order1, order2);
  }

  /*
   * Tests if order is open when there is no open order with specified id
   */
  @Test
  public void testGetAmountOfBaseCurrencyToBuy() throws Exception {

    expect(market.getBaseCurrency()).andReturn("USD").anyTimes();
    expect(market.getCounterCurrency()).andReturn("BTC").anyTimes();
    expect(tradingApi.getImplName()).andReturn("Bitstamp").anyTimes();
    BigDecimal lastTradePrice = new BigDecimal("1234.56");
    expect(tradingApi.roundValue(BigDecimal.valueOf(0.0810005184033178)))
            .andReturn(BigDecimal.valueOf(0.08100052));
    expect(tradingApi.getLatestMarketPrice(MARKET_ID)).andReturn(lastTradePrice);

    replay(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);

    final TradingContext context = new TradingContext(tradingApi, market);
    BigDecimal expCurrencyToBuy = new BigDecimal("0.08100052");
    assertEquals(
            expCurrencyToBuy,
            context.getAmountOfBaseCurrency(new BigDecimal("100.0"))
    );

    verify(market, tradingApi, config, marketOrderBook, marketBuyOrder, marketSellOrder);
  }

}
