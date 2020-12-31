package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.trading.api.BalanceInfo;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrderBook;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulatedTradingApi implements TradingApi {

  private final String name;
  private final double[] seriesData;
  private int counter = 0;
  private int index = 0;
  private long orderId = 0L;
  private List<OpenOrder> openOrders;
  private final BalanceInfo balanceInfo;

  // Start with $1000 - half in USD and half in BTC.
  // 500USD / 25000 USD/BTC = 0.02
  private static final Double INITIAL_BTC_BALANCE = 0.02;
  private static final Double INITIAL_USD_BALANCE = 500.0;
  private static final BigDecimal ZERO = BigDecimal.valueOf(0);

  public SimulatedTradingApi(String name,
                             double[] seriesData) {
    this(name, INITIAL_BTC_BALANCE, INITIAL_USD_BALANCE, seriesData);
  }

  /**
   * simulated api.
   */
  public SimulatedTradingApi(String name,
                             Double initialUsd, Double initialBtc,
                             double[] seriesData) {
    this.name = name;
    this.seriesData = seriesData;
    this.openOrders = new ArrayList<>();

    Map<String, BigDecimal> availableBalances = Map.of(
            "BTC", BigDecimal.valueOf(initialUsd),
            "USD", BigDecimal.valueOf(initialBtc)
    );
    balanceInfo = new StubBalanceInfo(availableBalances);
  }

  @Override
  public String getImplName() {
    return name;
  }

  public int getNumSimulatedCycles() {
    return seriesData.length;
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId)
          throws ExchangeNetworkException, TradingApiException {

    index = counter / 2; // this method is called twice each trading cycle
    if (index >= seriesData.length) {
      throw new IllegalStateException("index " + index + " exceeded size of seriesData");
    }
    counter++;
    // call with new price from time series each time
    return new StubMarketOrderBook(marketId, seriesData[index]);
  }

  @Override
  public List<OpenOrder> getYourOpenOrders(String marketId) {
    return openOrders;
  }

  @Override
  public String createOrder(String marketId, OrderType orderType,
                            BigDecimal quantity, BigDecimal price) {
    orderId++;
    OpenOrder order = new StubOpenOrder(Long.toString(orderId),
            marketId, orderType, price, quantity);
    openOrders.add(order);

    return Long.toString(orderId);
  }

  @Override
  public boolean cancelOrder(String orderId, String marketId) {
    openOrders = openOrders.stream()
            .filter(order -> !order.getId().equals(orderId)).collect(Collectors.toList());
    return false;
  }

  @Override
  public BigDecimal getLatestMarketPrice(String marketId) {
    double latestValue = index >= seriesData.length
            ? seriesData[seriesData.length - 1] : seriesData[index];
    return BigDecimal.valueOf(latestValue);
  }

  @Override
  public BalanceInfo getBalanceInfo() {
    return balanceInfo;
  }

  @Override
  public BigDecimal getPercentageOfBuyOrderTakenForExchangeFee(String marketId) {
    return ZERO;
  }

  @Override
  public BigDecimal getPercentageOfSellOrderTakenForExchangeFee(String marketId) {
    return ZERO;
  }
}
