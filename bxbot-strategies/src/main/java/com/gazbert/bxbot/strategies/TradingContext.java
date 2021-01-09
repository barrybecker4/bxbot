package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.OpenOrder;
import com.gazbert.bxbot.trading.api.OrderType;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Encapsulates market and tradingApi.
 */
public class TradingContext {

  private static final Logger LOG = LogManager.getLogger();

  private final TradingApi tradingApi;
  private final Market market;

  TradingContext(TradingApi tradingApi, Market market) {
    this.tradingApi = tradingApi;
    this.market = market;
  }

  public String getExchangeApi() {
    return tradingApi.getImplName();
  }

  public String getMarketName() {
    return market.getName();
  }

  // typically "BTC"
  public String getBaseCurrency() {
    return market.getBaseCurrency();
  }

  // typically "USD"
  public String getCounterCurrency() {
    return market.getCounterCurrency();
  }

  public List<MarketOrder> getBuyOrders() throws TradingApiException, ExchangeNetworkException {
    return tradingApi.getMarketOrders(market.getId()).getBuyOrders();
  }

  public List<MarketOrder> getSellOrders() throws TradingApiException, ExchangeNetworkException {
    return tradingApi.getMarketOrders(market.getId()).getSellOrders();
  }

  /**
   * Round based on trading API rules.
   *
   * @return rounded value based on the trading API's required rounding rules
   */
  public BigDecimal roundValue(BigDecimal value) {
    return tradingApi.roundValue(value);
  }

  /**
   * Amount of base currency available to trade.
   *
   * @return amount of base currency available to trade.
   */
  public BigDecimal getBaseCurrencyBalance()
          throws TradingApiException, ExchangeNetworkException {
    return getAvailableBalances().get(getBaseCurrency());
  }

  public BigDecimal getCounterCurrencyBalance()
          throws TradingApiException, ExchangeNetworkException {
    return getAvailableBalances().get(getCounterCurrency());
  }

  // call this version if you need both base and counter currency balances
  // to avoid multiple api calls
  public Map<String, BigDecimal> getAvailableBalances()
          throws TradingApiException, ExchangeNetworkException {
    return tradingApi.getBalanceInfo().getBalancesAvailable();
  }

  /**
   * Send the buy order to the exchange.
   * @return new order state
   */
  public OrderState sendBuyOrder(BigDecimal amountOfBaseCurrencyToBuy, BigDecimal bidPrice)
          throws TradingApiException, ExchangeNetworkException {

    LOG.info(() -> market.getName()
            + " Sending BUY order to exchange with bid=" + bidPrice + " --->");

    String id = tradingApi.createOrder(market.getId(),
            OrderType.BUY, amountOfBaseCurrencyToBuy, bidPrice);

    LOG.info(() -> market.getName() + " BUY Order sent successfully. ID: " + id);

    return new OrderState(id, OrderType.BUY, bidPrice, amountOfBaseCurrencyToBuy);
  }

  /**
   * Send the sell order to the exchange.
   * @return new order state
   */
  public OrderState sendSellOrder(BigDecimal amountOfBaseCurrencyToSell, BigDecimal askPrice)
          throws TradingApiException, ExchangeNetworkException {

    LOG.info(() -> market.getName()
            + " Placing new SELL order at ask price ["
            + PriceUtil.formatPrice(askPrice) + "]");

    LOG.info(() -> market.getName() + " Sending new SELL order to exchange --->");

    // Build the new sell order
    String id = tradingApi.createOrder(market.getId(),
            OrderType.SELL, amountOfBaseCurrencyToSell, askPrice);

    LOG.info(() -> market.getName() + " New SELL Order sent successfully. ID: " + id);

    return new OrderState(id, OrderType.SELL, askPrice, amountOfBaseCurrencyToSell);
  }

  /**
   * Examine current market orders to see if specified order is open.
   * @return true if the specified order is still outstanding/open on the exchange.
   */
  public boolean isOrderOpen(String orderId)
          throws TradingApiException, ExchangeNetworkException {
    final List<OpenOrder> myOpenOrders = tradingApi.getYourOpenOrders(market.getId());
    for (final OpenOrder myOrder : myOpenOrders) {
      if (myOrder.getId().equals(orderId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Use current open market orders to see if any of the orders in a specified order list
   * have been filled. If the id is not in the current open market orders, then we assume
   * that it was filled.
   *
   * @return set of order ids that were filled. There could be 0, but it would be
   *     very unusual for there to be more than one.
   */
  public Set<String> findFilledOrderIds(Set<String> orderIds)
          throws TradingApiException, ExchangeNetworkException {
    final List<OpenOrder> myOpenOrders = tradingApi.getYourOpenOrders(market.getId());
    Set<String> myOrderIds =
            myOpenOrders.stream().map(OpenOrder::getId).collect(Collectors.toSet());
    myOrderIds.removeAll(orderIds);
    return myOrderIds;
  }

  /**
   * Returns amount of base currency (e.g. BTC) corresponding to an amount of
   * counter currency (e.g. USD).
   *
   * @param amountOfCounterCurrency the amount of counter currency (e.g. USD)
   *      we have to trade (buy) with.
   * @return the amount of base currency (e.g. BTC) given counter currency (e.g. USD) amount.
   * @throws TradingApiException if an unexpected error occurred contacting the exchange.
   * @throws ExchangeNetworkException if a request to the exchange has timed out.
   */
  public BigDecimal getAmountOfBaseCurrency(
          BigDecimal amountOfCounterCurrency)
          throws TradingApiException, ExchangeNetworkException {

    // Fetch the last trade price
    final BigDecimal lastTradePriceInUsdForOneBtc = tradingApi.getLatestMarketPrice(market.getId());

    LOG.info(() -> market.getName()
            + " Last trade price for 1 " + market.getBaseCurrency() + " was: "
            + PriceUtil.formatPrice(lastTradePriceInUsdForOneBtc) + " "
            + market.getCounterCurrency());

    BigDecimal unroundedBaseCurrency = amountOfCounterCurrency
            .divide(lastTradePriceInUsdForOneBtc, 16, RoundingMode.HALF_UP);
    final BigDecimal amountOfBaseCurrencyToBuy =
            roundValue(unroundedBaseCurrency);

    LOG.info(() -> market.getName()
            + " Amount of base currency (" + market.getBaseCurrency() + ") corresponding to "
            + PriceUtil.formatPrice(amountOfCounterCurrency) + " "
            + market.getCounterCurrency() + " based on last market trade price: "
            + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }

}
