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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimulatedTradingApi implements TradingApi {

  private final String name;
  private final double[] seriesData;
  private int index = 0;
  private long orderId = 0L;
  private List<OpenOrder> openOrders;
  private final BalanceInfo balanceInfo;

  private static String BTC = "BTC";
  private static String USD = "USD";

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

    Map<String, BigDecimal> availableBalances = new HashMap<>();
    availableBalances.put(BTC, BigDecimal.valueOf(initialUsd));
    availableBalances.put(USD, BigDecimal.valueOf(initialBtc));

    balanceInfo = new StubBalanceInfo(availableBalances);
  }

  @Override
  public String getImplName() {
    return name;
  }

  public int getNumSimulatedCycles() {
    return seriesData.length;
  }

  /**
   * Advance to nex series dat point.
   * Close any open orders where the current threshold has based the order bid/ask
   * Then remove that order from the list.
   */
  public void advanceToNextTradingCycle() {

    double newPrice = seriesData[index];
    index++;

    openOrders = openOrders.stream()
            .filter(order -> {
              boolean keep;
              if (order.getType() == OrderType.BUY) {
                keep = order.getPrice().doubleValue() > newPrice;
              } else { // SELL
                keep = order.getPrice().doubleValue() < newPrice;
              }
              if (!keep) {
                updateBalancesForExecutedOrder(order);
              }
              return keep;
            })
            .collect(Collectors.toList());
  }

  private void updateBalancesForExecutedOrder(OpenOrder order) {
    Map<String, BigDecimal> availableBalances = balanceInfo.getBalancesAvailable();
    //System.out.println("executing simulated order: " + order);
    if (order.getType() == OrderType.BUY) {
      // buying BTC, adjust the balances
      BigDecimal newBtcBalance =
              availableBalances.get(BTC).add(order.getQuantity());
      availableBalances.put(BTC, newBtcBalance);
      BigDecimal newUsdBalance =
              availableBalances.get(USD).subtract(order.getQuantity().multiply(order.getPrice()));
      availableBalances.put(USD, newUsdBalance);
    } else {
      // selling BTC, adjust the balances
      BigDecimal newBtcBalance =
              availableBalances.get(BTC).subtract(order.getQuantity());
      availableBalances.put(BTC, newBtcBalance);
      BigDecimal newUsdBalance =
              availableBalances.get(USD).add(order.getQuantity().multiply(order.getPrice()));
      availableBalances.put(USD, newUsdBalance);
    }
  }

  @Override
  public MarketOrderBook getMarketOrders(String marketId) {

    if (index >= seriesData.length) {
      throw new IllegalStateException("index " + index + " exceeded size of seriesData");
    }
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
