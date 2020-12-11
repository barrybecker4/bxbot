package com.gazbert.bxbot.strategies;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.TradingApi;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class TestTradingContext {

  private static final String MARKET_ID = "btc_usd";
  private static final String BASE_CURRENCY = "BTC";
  private static final String COUNTER_CURRENCY = "USD";

  private static final String CONFIG_ITEM_COUNTER_CURRENCY_BUY_ORDER_AMOUNT = "20"; // USD amount
  private static final String CONFIG_ITEM_MINIMUM_PERCENTAGE_GAIN = "2";
  private static final String ORDER_ID = "45345346";

  private TradingApi tradingApi;
  private Market market;
  private StrategyConfig config;

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
    config = createMock(StrategyConfig.class);

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

}
