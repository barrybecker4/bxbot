package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
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
import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;

/**
 * Simple <a href="http://www.investopedia.com/articles/trading/02/081902.asp">scalping strategy</a>
 * to show how to use the Trading API.
 *
 * @author Barry Becker
 */
@Configurable
@Component("barrysTradingStrategy") // used to load the strategy using Spring bean injection
public class BarrysTradingStrategy implements TradingStrategy {

  private static final Logger LOG = LogManager.getLogger();

  private static final String DECIMAL_FORMAT = "#.########";

  private TradingApi tradingApi;

  private Market market;

  private BarrysTradingStrategyConfig strategyConfig;

  private OrderState lastOrder;

  @Autowired
  private TransactionsRepository transactionRepo;

  /**
   * Called once by the Trading Engine when the bot starts up.
   *
   * @param tradingApi the Trading API. Use this to make trades and stuff.
   * @param market the market the strategy is currently running on -
   *               you wire this up in the markets.yaml and strategies.yaml files.
   * @param config Contains any (optional) config you set up in the strategies.yaml file.
   */
  @Override
  public void init(TradingApi tradingApi, Market market, StrategyConfig config) {
    this.tradingApi = tradingApi;
    this.market = market;
    strategyConfig = new BarrysTradingStrategyConfig(config);
    LOG.info(() -> "Barry's Trading Strategy was initialised successfully!");
    demoDb();
  }

  /**
   * Exercises the db.
   */
  private void demoDb() {
    if (transactionRepo == null) {
      LOG.info(() -> "No TransactionRepo!!!!!!!!");
      return;
    }
    transactionRepo.save(new TransactionEntry(1L,"Jack", "Bauer", 1.234));
    transactionRepo.save(new TransactionEntry(2L, "Chloe", "O'Brian", 2.345));
    transactionRepo.save(new TransactionEntry(3L, "Kim", "Bauer", 3.456));
    transactionRepo.save(new TransactionEntry(4L, "David", "Palmer", 4.567));
    transactionRepo.save(new TransactionEntry(5L, "Michelle", "Dessler", 5.678));

    // fetch all transactions
    LOG.info(() -> "Transactions found with findAll():");
    LOG.info(() -> "-------------------------------");
    for (TransactionEntry txn : transactionRepo.findAll()) {
      LOG.info(txn::toString);
    }
    LOG.info(() -> "");

    // fetch an individual customer by ID
    Optional<TransactionEntry> txn = transactionRepo.findById(1L);
    LOG.info(() -> "Transaction found with findById(1L):");
    LOG.info(() -> "--------------------------------");
    LOG.info(txn::toString);
    LOG.info(() -> "");

    // fetch customers by last name
    LOG.info(() -> "Transaction found with findByType('Bauer'):");
    LOG.info(() -> "--------------------------------------------");
    transactionRepo.findByType("Bauer").forEach(bauer -> {
      LOG.info(bauer::toString);
    });
    LOG.info(() -> "");
  }

  /**
   * The main execution method of the Trading Strategy.
   * It is called by the Trading Engine during each trade cycle, e.g. every 60s. The trade cycle
   * is configured in the {project-root}/config/engine.yaml file.
   *
   * @throws StrategyException if something unexpected occurs. This tells the Trading Engine to
   *     shutdown the bot immediately to help prevent unexpected losses.
   */
  @Override
  public void execute() throws StrategyException {
    LOG.info(() -> market.getName() + " Checking order status...");

    try {
      executeStrategy();

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here.
      // We are just going to LOG.it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " Failed to get market orders because Exchange threw network exception. "
                  + "Waiting until next trade cycle.",
          e);
    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          market.getName()
              + " Failed to get market orders because Exchange threw TradingApi exception. "
              + "Telling Trading Engine to shutdown bot!",
          e);
      throw new StrategyException(e);
    }
  }

  private void executeStrategy()
          throws ExchangeNetworkException, TradingApiException, StrategyException {

    // Grab the latest order book for the market.
    final MarketOrderBook orderBook = tradingApi.getMarketOrders(market.getId());

    final List<MarketOrder> buyOrders = orderBook.getBuyOrders();
    if (buyOrders.isEmpty()) {
      LOG.warn(() -> "Exchange returned empty Buy Orders. Ignoring this trade window. OrderBook: "
              + orderBook);
      return;
    }

    final List<MarketOrder> sellOrders = orderBook.getSellOrders();
    if (sellOrders.isEmpty()) {
      LOG.warn(() -> "Exchange returned empty Sell Orders. Ignoring this trade window. OrderBook: "
              + orderBook);
      return;
    }

    // Get the current BID and ASK spot prices.
    final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
    final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();

    LOG.info(() ->  market.getName()
            + " Current BID price="
            + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice));
    LOG.info(() -> market.getName()
            + " Current ASK price="
            + new DecimalFormat(DECIMAL_FORMAT).format(currentAskPrice));

    // Is this the first time the Strategy has been called? If yes, we initialise the OrderState
    // so we can keep track of orders during later trace cycles.
    if (lastOrder == null) {
      LOG.info(() -> market.getName()
             + " First time Strategy has been called - creating new OrderState object.");
      lastOrder = new OrderState();
    }

    // Always handy to LOG.what the last order was during each trace cycle.
    LOG.info(() -> market.getName() + " Last Order was: " + lastOrder);

    // Execute the appropriate algorithm based on the last order type.
    if (lastOrder.type == null) {
      executeWhenLastOrderWasNone(currentBidPrice);
      return;
    }
    switch (lastOrder.type) {
      case BUY:
        executeWhenLastOrderWasBuy();
        break;
      case SELL:
        executeWhenLastOrderWasSell(currentBidPrice, currentAskPrice);
        break;
      default: throw new TradingApiException("Invalid Order type: " + lastOrder.type);
    }
  }

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with a
   * buy order at current BID price.
   *
   * @param currentBidPrice the current market BID price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {
    LOG.info(
        () ->
            market.getName()
                + " OrderType is NONE - placing new BUY order at ["
                + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                + "]");

    try {
      // Calculate the amount of base currency (BTC) to buy for given amount of counter currency
      // (USD).
      final BigDecimal amountOfBaseCurrencyToBuy =
          getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                  strategyConfig.getCounterCurrencyBuyOrderAmount());

      // Send the order to the exchange
      LOG.info(() -> market.getName() + " Sending initial BUY order to exchange --->");

      lastOrder.id =
          tradingApi.createOrder(
              market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);

      LOG.info(
          () -> market.getName() + " Initial BUY Order sent successfully. ID: " + lastOrder.id);

      // update last order details
      lastOrder.price = currentBidPrice;
      lastOrder.type = OrderType.BUY;
      lastOrder.amount = amountOfBaseCurrencyToBuy;

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order
      // actually made it to the exchange? And if not, resend it...
      // We are just going to LOG.it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " Initial order to BUY base currency failed because Exchange threw network "
                  + "exception. Waiting until next trade cycle.",
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          () ->
              market.getName()
                  + " Initial order to BUY base currency failed because Exchange threw TradingApi "
                  + "exception. Telling Trading Engine to shutdown bot!",
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchanges was a BUY.
   *
   * <p>If last buy order filled, we try and sell at a profit.
   *
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeWhenLastOrderWasBuy() throws StrategyException {
    try {
      // Fetch our current open orders and see if the buy order is still outstanding/open on the
      // exchange
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        LOG.info(
            () ->
                market.getName()
                    + " ^^^ Yay!!! Last BUY Order Id ["
                    + lastOrder.id
                    + "] filled at ["
                    + lastOrder.price
                    + "]");

        /*
         * The last buy order was filled, so lets see if we can send a new sell order.
         *
         * IMPORTANT - new sell order ASK price must be > (last order price + exchange fees)
         *             because:
         *
         * 1. If we put sell amount in as same amount as previous buy, the exchange barfs because
         *    we don't have enough units to cover the transaction fee.
         * 2. We could end up selling at a loss.
         *
         * For this example strategy, we're just going to add 2% (taken from the
         * 'minimum-percentage-gain' config item in the {project-root}/config/strategies.yaml
         * config file) on top of previous bid price to make a little profit and cover the exchange
         * fees.
         *
         * Your algo will have other ideas on how much profit to make and when to apply the
         * exchange fees - you could try calling the
         * TradingApi#getPercentageOfBuyOrderTakenForExchangeFee() and
         * TradingApi#getPercentageOfSellOrderTakenForExchangeFee() when calculating the order to
         * send to the exchange...
         */
        LOG.info(
            () ->
                market.getName()
                    + " Percentage profit (in decimal) to make for the sell order is: "
                    + strategyConfig.getMinimumPercentageGain());

        final BigDecimal amountToAdd =
                lastOrder.price.multiply(strategyConfig.getMinimumPercentageGain());
        LOG.info(() -> market.getName()
                + " Amount to add to last buy order fill price: " + amountToAdd);

        // Most exchanges (if not all) use 8 decimal places.
        // It's usually best to round up the ASK price in your calculations to maximise gains.
        final BigDecimal newAskPrice =
            lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);
        LOG.info(
            () ->
                market.getName()
                    + " Placing new SELL order at ask price ["
                    + new DecimalFormat(DECIMAL_FORMAT).format(newAskPrice)
                    + "]");

        LOG.info(() -> market.getName() + " Sending new SELL order to exchange --->");

        // Build the new sell order
        lastOrder.id =
            tradingApi.createOrder(market.getId(), OrderType.SELL, lastOrder.amount, newAskPrice);
        LOG.info(() -> market.getName() + " New SELL Order sent successfully. ID: " + lastOrder.id);

        // update last order state
        lastOrder.price = newAskPrice;
        lastOrder.type = OrderType.SELL;
      } else {

        /*
         * BUY order has not filled yet.
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
         * has gone up and we've been outbid and have a stuck buy order. In which case, we have to
         * wait for the market to fall for the order to fill... or you could tweak this code to
         * cancel the current order and raise your bid - remember to deal with any part-filled
         * orders!
         */
        LOG.info(
            () ->
                market.getName()
                    + " !!! Still have BUY Order "
                    + lastOrder.id
                    + " waiting to fill at ["
                    + lastOrder.price
                    + "] - holding last BUY order...");
      }

    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order
      // actually
      // made it to the exchange? And if not, resend it...
      // We are just going to LOG.it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " New Order to SELL base currency failed because Exchange threw network "
                  + "exception. Waiting until next trade cycle. Last Order: "
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          () ->
              market.getName()
                  + " New order to SELL base currency failed because Exchange threw TradingApi "
                  + "exception. Telling Trading Engine to shutdown bot! Last Order: "
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Algo for executing when last order we placed on the exchange was a SELL.
   *
   * <p>If last sell order filled, we send a new buy order to the exchange.
   *
   * @param currentBidPrice the current market BID price.
   * @param currentAskPrice the current market ASK price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeWhenLastOrderWasSell(
      BigDecimal currentBidPrice, BigDecimal currentAskPrice) throws StrategyException {
    try {
      final List<OpenOrder> myOrders = tradingApi.getYourOpenOrders(market.getId());
      boolean lastOrderFound = false;
      for (final OpenOrder myOrder : myOrders) {
        if (myOrder.getId().equals(lastOrder.id)) {
          lastOrderFound = true;
          break;
        }
      }

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        LOG.info(
            () ->
                market.getName()
                    + " ^^^ Yay!!! Last SELL Order Id ["
                    + lastOrder.id
                    + "] filled at ["
                    + lastOrder.price
                    + "]");

        // Get amount of base currency (BTC) we can buy for given counter currency (USD) amount.
        final BigDecimal amountOfBaseCurrencyToBuy =
            getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
                strategyConfig.getCounterCurrencyBuyOrderAmount());

        LOG.info(
            () ->
                market.getName()
                    + " Placing new BUY order at bid price ["
                    + new DecimalFormat(DECIMAL_FORMAT).format(currentBidPrice)
                    + "]");

        LOG.info(() -> market.getName() + " Sending new BUY order to exchange --->");

        // Send the buy order to the exchange.
        lastOrder.id =
            tradingApi.createOrder(
                market.getId(), OrderType.BUY, amountOfBaseCurrencyToBuy, currentBidPrice);
        LOG.info(() -> market.getName() + " New BUY Order sent successfully. ID: " + lastOrder.id);

        // update last order details
        lastOrder.price = currentBidPrice;
        lastOrder.type = OrderType.BUY;
        lastOrder.amount = amountOfBaseCurrencyToBuy;
      } else {

        /*
         * SELL order not filled yet.
         * Could be nobody has jumped on it yet... or the order is only part filled... or market
         * has gone down and we've been undercut and have a stuck sell order. In which case, we
         * have to wait for market to recover for the order to fill... or you could tweak this
         * code to cancel the current order and lower your ask - remember to deal with any
         * part-filled orders!
         */
        if (currentAskPrice.compareTo(lastOrder.price) < 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " <<< Current ask price ["
                      + currentAskPrice
                      + "] is LOWER then last order price ["
                      + lastOrder.price
                      + "] - holding last SELL order...");

        } else if (currentAskPrice.compareTo(lastOrder.price) > 0) {
          LOG.error(
              () ->
                  market.getName()
                      + " >>> Current ask price ["
                      + currentAskPrice
                      + "] is HIGHER than last order price ["
                      + lastOrder.price
                      + "] - IMPOSSIBLE! BX-bot must have sold?????");

        } else if (currentAskPrice.compareTo(lastOrder.price) == 0) {
          LOG.info(
              () ->
                  market.getName()
                      + " === Current ask price ["
                      + currentAskPrice
                      + "] is EQUAL to last order price ["
                      + lastOrder.price
                      + "] - holding last SELL order...");
        }
      }
    } catch (ExchangeNetworkException e) {
      // Your timeout handling code could go here, e.g. you might want to check if the order
      // actually made it to the exchange? And if not, resend it...
      // We are just going to log it and swallow it, and wait for next trade cycle.
      LOG.error(
          () ->
              market.getName()
                  + " New Order to BUY base currency failed because Exchange threw network "
                  + "exception. Waiting until next trade cycle. Last Order: "
                  + lastOrder,
          e);

    } catch (TradingApiException e) {
      // Your error handling code could go here...
      // We are just going to re-throw as StrategyException for engine to deal with - it will
      // shutdown the bot.
      LOG.error(
          () ->
              market.getName()
                  + " New order to BUY base currency failed because Exchange threw TradingApi "
                  + "exception. Telling Trading Engine to shutdown bot! Last Order: "
                  + lastOrder,
          e);
      throw new StrategyException(e);
    }
  }

  /**
   * Returns amount of base currency (BTC) to buy for a given amount of counter currency (USD) based
   * on last market trade price.
   *
   * @param amountOfCounterCurrencyToTrade the amount of counter currency (USD) we have to trade
   *     (buy) with.
   * @return the amount of base currency (BTC) we can buy for the given counter currency (USD)
   *     amount.
   * @throws TradingApiException if an unexpected error occurred contacting the exchange.
   * @throws ExchangeNetworkException if a request to the exchange has timed out.
   */
  private BigDecimal getAmountOfBaseCurrencyToBuyForGivenCounterCurrencyAmount(
      BigDecimal amountOfCounterCurrencyToTrade)
      throws TradingApiException, ExchangeNetworkException {

    LOG.info(
        () ->
            market.getName()
                + " Calculating amount of base currency (BTC) to buy for amount of counter "
                + "currency "
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency());

    // Fetch the last trade price
    final BigDecimal lastTradePriceInUsdForOneBtc = tradingApi.getLatestMarketPrice(market.getId());
    LOG.info(
        () ->
            market.getName()
                + " Last trade price for 1 "
                + market.getBaseCurrency()
                + " was: "
                + new DecimalFormat(DECIMAL_FORMAT).format(lastTradePriceInUsdForOneBtc)
                + " "
                + market.getCounterCurrency());

    /*
     * Most exchanges (if not all) use 8 decimal places and typically round in favour of the
     * exchange. It's usually safest to round down the order quantity in your calculations.
     */
    final BigDecimal amountOfBaseCurrencyToBuy =
        amountOfCounterCurrencyToTrade.divide(
            lastTradePriceInUsdForOneBtc, 8, RoundingMode.HALF_DOWN);

    LOG.info(
        () ->
            market.getName()
                + " Amount of base currency ("
                + market.getBaseCurrency()
                + ") to BUY for "
                + new DecimalFormat(DECIMAL_FORMAT).format(amountOfCounterCurrencyToTrade)
                + " "
                + market.getCounterCurrency()
                + " based on last market trade price: "
                + amountOfBaseCurrencyToBuy);

    return amountOfBaseCurrencyToBuy;
  }

}
