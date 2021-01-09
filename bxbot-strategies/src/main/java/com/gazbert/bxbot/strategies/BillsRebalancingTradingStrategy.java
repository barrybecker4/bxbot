package com.gazbert.bxbot.strategies;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.FILLED;
import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;


/**
 * Adaption of
 * <a href="https://gist.github.com/wkeese/3c2a0c0292a185c01bf4d9dd144e843a">Bills rebalancing </a>.
 * strategy
 * @author Barry Becker
 */
@Configurable
@Component("billsRebalancingTradingStrategy") // for Spring bean injection
public class BillsRebalancingTradingStrategy extends AbstractTradingStrategy {

  private final Map<String, OrderState> openOrders = new HashMap<>();

  protected BaseStrategyConfig createTradingStrategyConfig(IStrategyConfigItems config) {
    return new BillsRebalancingTradingStrategyConfig(config);
  }

  /**
   * Main method called by Trading Engine during each trade cycle, e.g. every 60s.
   * The trade cycle is configured in the {project-root}/config/engine.yaml file.
   *
   * @throws StrategyException if something unexpected occurs. This tells the Trading Engine to
   *     shutdown the bot immediately to help prevent unexpected losses.
   */
  @Override
  public void execute() throws StrategyException {
    LOG.info(() -> context.getMarketName() + " Checking order status...");

    try {
      executeStrategy();
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("Failed to get market orders", e);
    } catch (TradingApiException e) {
      handleTradingApiException("Failed to get market orders", e);
    }
  }

  /**
   * add param for max outstanding orders.
   * add set of outstanding orders. Only add another of slots available.
   */
  private void executeStrategy()
          throws ExchangeNetworkException, TradingApiException, StrategyException {

    // Get buy/sell orders from latest order book for the market.
    final List<MarketOrder> buyOrders = context.getBuyOrders();
    final List<MarketOrder> sellOrders = context.getSellOrders();

    if (!hasOrders("Buy", buyOrders) || !hasOrders("Sell", sellOrders)) {
      LOG.info(() -> "No buy or sell orders (maybe market is closed)");
      return;
    }

    // Get the current BID and ASK spot prices.
    final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
    final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();
    logPrices(currentBidPrice, currentAskPrice);

    Map<String, BigDecimal> balances = context.getAvailableBalances();
    BigDecimal baseBalance = balances.get(context.getBaseCurrency());
    double counterBalance = balances.get(context.getCounterCurrency()).doubleValue();
    double baseCurrencyValue =
            context.getAmountOfBaseCurrency(baseBalance).doubleValue();
    double threshold = getConfig().getCounterCurrencyThreshold().doubleValue();

    reportFilledOrders();

    //System.out.println("counterBalance = " + counterBalance
    //        + " baseCurrencyValue = " + baseCurrencyValue);

    if (counterBalance < (baseCurrencyValue - threshold)) {
      BigDecimal baseToSell = BigDecimal.valueOf(
              (baseCurrencyValue - counterBalance) / (2 * currentAskPrice.doubleValue())
      );
      sendSellOrder(baseToSell, currentAskPrice);
    } else if (counterBalance > (baseCurrencyValue + threshold)) {
      BigDecimal counterCurrencyToSpend =
              BigDecimal.valueOf((counterBalance - baseCurrencyValue) / 2);
      sendBuyOrder(counterCurrencyToSpend, currentBidPrice);
    }
  }

  // If there are any orders that were filled, make sure that we report them in the database
  private void reportFilledOrders() throws TradingApiException, ExchangeNetworkException {

    Set<String> fillOrderIds = context.findFilledOrderIds(openOrders.keySet());

    for (String orderId : fillOrderIds) {
      OrderState filledOrder = openOrders.get(orderId);
      openOrders.remove(orderId);
      double value = filledOrder.amount.doubleValue() * filledOrder.price.doubleValue();
      LOG.info(() -> context.getMarketName()
              + " ^^^ Yay!!! " + filledOrder.type + " Order Id [" + orderId
              + "] with counter value " + value + " filled at ["
              + filledOrder.price + "]");
      persistTransaction(FILLED, filledOrder);
    }
  }

  private void sendBuyOrder(BigDecimal counterCurrencyToSpend, BigDecimal price)
          throws StrategyException {
    try {
      final BigDecimal amountOfBaseCurrencyToBuy =
              context.getAmountOfBaseCurrency(counterCurrencyToSpend);

      OrderState buyOrder = context.sendBuyOrder(amountOfBaseCurrencyToBuy, price);
      openOrders.put(buyOrder.id, buyOrder);

      persistTransaction(SENT, buyOrder);
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("Attempt to BUY base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("Attempt to BUY base currency failed", e);
    }
  }

  private BillsRebalancingTradingStrategyConfig getConfig() {
    return (BillsRebalancingTradingStrategyConfig) strategyConfig;
  }

  private void sendSellOrder(BigDecimal amountToSell, BigDecimal askPrice)
          throws TradingApiException, ExchangeNetworkException {
    OrderState sellOrder =
            context.sendSellOrder(amountToSell, askPrice);
    openOrders.put(sellOrder.id, sellOrder);

    persistTransaction(SENT, sellOrder);
  }

  private boolean hasOrders(String type, List<MarketOrder> orders) {
    if (orders.isEmpty()) {
      LOG.warn(() -> "Exchange returned no " + type + " Orders. Ignoring this trade window.");
    }
    return !orders.isEmpty();
  }

  private void logPrices(BigDecimal bidPrice, BigDecimal askPrice) {
    LOG.info(() ->  context.getMarketName()
            + " Current BID="
            + PriceUtil.formatPrice(bidPrice) + " ASK=" + PriceUtil.formatPrice(askPrice));
  }


  private void persistTransaction(TransactionEntry.Status status, OrderState order) {
    transactionRepo.save(
            new TransactionEntry(order.id, order.type.getStringValue(), status,
                    context.getMarketName(), order.amount, order.price,
                    strategyConfig.getStrategyId(), context.getExchangeApi()));
  }

  /**
   * Your timeout handling code could go here, e.g. you might want to check if the order
   * actually made it to the exchange? And if not, resend it...
   * We are just going to LOG.it and swallow it, and wait for next trade cycle.
   */
  private void handleExchangeNetworkException(String msg, ExchangeNetworkException e) {
    LOG.error(() -> context.getMarketName()
            + " " + msg + " because Exchange threw network "
            + "exception. Waiting until next trade cycle.", e);
  }

  /**
   * Your error handling code could go here...
   * Re-throw as StrategyException for engine to deal with - it will shutdown the bot.
   */
  private void handleTradingApiException(String msg, TradingApiException e)
          throws StrategyException {
    LOG.error(() -> context.getMarketName()
            + " " + msg + " because Exchange threw TradingApi "
            + "exception. Telling Trading Engine to shutdown bot!", e);
    throw new StrategyException(e);
  }
}
