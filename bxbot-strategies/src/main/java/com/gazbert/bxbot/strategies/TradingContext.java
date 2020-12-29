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
import java.util.List;
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

  // typically BTC
  public String getBaseCurrency() {
    return market.getBaseCurrency();
  }

  public List<MarketOrder> getBuyOrders() throws TradingApiException, ExchangeNetworkException {
    return tradingApi.getMarketOrders(market.getId()).getBuyOrders();
  }

  public List<MarketOrder> getSellOrders() throws TradingApiException, ExchangeNetworkException {
    return tradingApi.getMarketOrders(market.getId()).getSellOrders();
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
    final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
    for (final OpenOrder myOrder : myOrders) {
      if (myOrder.getId().equals(orderId)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns amount of base currency (e.g. BTC) to buy for a given amount of
   * counter currency (e.g. USD) based on last market trade price.
   *
   * @param amountOfCounterCurrencyToTrade the amount of counter currency (e.g. USD)
   *      we have to trade (buy) with.
   * @return the amount of base currency (e.g. BTC) we can buy for the given
   *     counter currency (e.g. USD) amount.
   * @throws TradingApiException if an unexpected error occurred contacting the exchange.
   * @throws ExchangeNetworkException if a request to the exchange has timed out.
   */
  public BigDecimal getAmountOfBaseCurrencyToBuy(
          BigDecimal amountOfCounterCurrencyToTrade)
          throws TradingApiException, ExchangeNetworkException {

    // Fetch the last trade price
    final BigDecimal lastTradePriceInUsdForOneBtc = tradingApi.getLatestMarketPrice(market.getId());

    LOG.info(() -> market.getName()
            + " Last trade price for 1 " + market.getBaseCurrency() + " was: "
            + PriceUtil.formatPrice(lastTradePriceInUsdForOneBtc) + " "
            + market.getCounterCurrency());

    /*
     * Most exchanges (if not all) use 8 decimal places and typically round in favour of the
     * exchange. It's usually safest to round down the order quantity in your calculations.
     */
    final BigDecimal amountOfBaseCurrencyToBuy =
            amountOfCounterCurrencyToTrade.divide(
                    lastTradePriceInUsdForOneBtc, 8, RoundingMode.HALF_DOWN);

    LOG.info(() -> market.getName()
            + " Amount of base currency (" + market.getBaseCurrency() + ") to BUY for "
            + PriceUtil.formatPrice(amountOfCounterCurrencyToTrade) + " "
            + market.getCounterCurrency() + " based on last market trade price: "
            + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }

}
