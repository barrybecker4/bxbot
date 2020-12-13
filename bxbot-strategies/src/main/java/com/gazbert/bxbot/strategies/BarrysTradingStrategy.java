package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategy.api.StrategyConfig;
import com.gazbert.bxbot.strategy.api.StrategyException;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.ExchangeNetworkException;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.MarketOrder;
import com.gazbert.bxbot.trading.api.TradingApi;
import com.gazbert.bxbot.trading.api.TradingApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

  private TradingContext context;
  private BarrysTradingStrategyConfig strategyConfig;
  private OrderState lastOrder;

  // Move this to TradingContext
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
    init(new TradingContext(tradingApi, market), config);
  }

  /**
   * Used by tests.
   */
  public void init(TradingContext context, StrategyConfig config) {
    this.context = context;
    strategyConfig = new BarrysTradingStrategyConfig(config);
    LOG.info(() -> "Barry's Trading Strategy was initialised successfully!");
    demoDb(); // temporary for testing the recording of transactions
  }

  /**
   * Exercises the db.
   */
  private void demoDb() {
    if (transactionRepo == null) {
      LOG.info(() -> "No TransactionRepo!!");
      return;
    }
    transactionRepo.save(new TransactionEntry("Jack", "Bauer", 1.234));
    transactionRepo.save(new TransactionEntry("Chloe", "O'Brian", 2.345));
    transactionRepo.save(new TransactionEntry("Kim", "Bauer", 3.456));
    transactionRepo.save(new TransactionEntry("David", "Palmer", 4.567));
    transactionRepo.save(new TransactionEntry("Michelle", "Dessler", 5.678));

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

  private void executeStrategy()
          throws ExchangeNetworkException, TradingApiException, StrategyException {

    // Get buy/sell orders from latest order book for the market.
    final List<MarketOrder> buyOrders = context.getBuyOrders();
    final List<MarketOrder> sellOrders = context.getSellOrders();

    if (!hasOrders("Buy", buyOrders) || !hasOrders("Sell", sellOrders)) {
      return;
    }

    // Get the current BID and ASK spot prices.
    final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
    final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();
    logPrices(currentBidPrice, currentAskPrice);

    initializeLastOrderIfNeeded();

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
   * Is this the first time the Strategy has been called? If yes, initialise the OrderState
   * so we can keep track of orders during later trace cycles.
   */
  private void initializeLastOrderIfNeeded() {
    if (lastOrder == null) {
      LOG.info(() -> context.getMarketName()
              + " First time Strategy has been called - creating new OrderState object.");
      lastOrder = new OrderState();
    }

    // Always handy to LOG.what the last order was during each trace cycle.
    LOG.info(() -> context.getMarketName() + " Last Order was: " + lastOrder);
  }

  private boolean hasOrders(String type, List<MarketOrder> orders) {
    if (orders.isEmpty()) {
      LOG.warn(() -> "Exchange returned no " + type + " Orders. Ignoring this trade window.");
    }
    return orders.isEmpty();
  }

  private void logPrices(BigDecimal bidPrice, BigDecimal askPrice) {
    LOG.info(() ->  context.getMarketName()
            + " Current BID="
            + PriceUtil.formatPrice(bidPrice) + " ASK=" + PriceUtil.formatPrice(askPrice));
  }

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with
   * a buy order at current BID price.
   *
   * @param currentBidPrice the current market BID price.
   * @throws StrategyException if an unexpected exception is received from the Exchange Adapter.
   *     Throwing this exception indicates we want the Trading Engine to shutdown the bot.
   */
  private void executeWhenLastOrderWasNone(BigDecimal currentBidPrice)
      throws StrategyException {
    LOG.info(() -> context.getMarketName()
                + " OrderType is NONE - placing new BUY order at ["
                + PriceUtil.formatPrice(currentBidPrice)
                + "]");

    try {
      // Calculate amount of base currency (BTC) to buy for given amount of counter currency (USD).
      final BigDecimal amountOfBaseCurrencyToBuy =
          context.getAmountOfBaseCurrencyToBuy(
                  strategyConfig.getCounterCurrencyBuyOrderAmount());

      lastOrder = context.sendBuyOrder(amountOfBaseCurrencyToBuy, currentBidPrice);
      transactionRepo.save(new TransactionEntry("Jack", "Bauer", 1.234));

    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("Initial Order to BUY base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("Initial order to BUY base currency failed", e);
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
      boolean lastOrderFound = context.isOrderOpen(lastOrder.id);

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        LOG.info(() -> context.getMarketName()
                + " ^^^ Yay!!! Last BUY Order Id [" + lastOrder.id + "] filled at ["
                + lastOrder.price + "]");

        /*
         * The last buy order was filled, so lets see if we can send a new sell order.
         * IMPORTANT - new sell order ASK price must be > (last order price + exchange fees)
         *             because:
         * 1. If we put sell amount in as same amount as previous buy, the exchange barfs if
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
        final BigDecimal amountToAdd =
                lastOrder.price.multiply(strategyConfig.getMinimumPercentageGain());
        LOG.info(() -> context.getMarketName()
                + " Amount to add to last buy order fill price: " + amountToAdd);

        // Most exchanges (if not all) use 8 decimal places.
        // It's usually best to round up the ASK price in your calculations to maximise gains.
        final BigDecimal newAskPrice =
            lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);

        lastOrder = context.sendSellOrder(lastOrder.amount, newAskPrice);
      } else {
        logBuyNotFilledYet();
      }
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("New Order to SELL base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("New order to SELL base currency failed", e);
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
      boolean lastOrderFound = context.isOrderOpen(lastOrder.id);

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        LOG.info(() -> context.getMarketName()
                    + " ^^^ Yay!!! Last SELL Order Id ["
                    + lastOrder.id + "] filled at [" + lastOrder.price + "]");

        // Get amount of base currency (BTC) we can buy for given counter currency (USD) amount.
        final BigDecimal amountOfBaseCurrencyToBuy =
            context.getAmountOfBaseCurrencyToBuy(
                strategyConfig.getCounterCurrencyBuyOrderAmount());

        lastOrder = context.sendBuyOrder(amountOfBaseCurrencyToBuy, currentBidPrice);
      } else {
        logSellOrderNotFilledYet(currentAskPrice);
      }
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("New Order to BUY base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("New order to BUY base currency failed", e);
    }
  }

  /*
   * Could be nobody has jumped on it yet, or the order is only part filled, or market
   * has gone up and we've been outbid and have a stuck buy order. If stuck, we have to
   * wait for the market to fall for the order to fill, or you could tweak this code to
   * cancel the current order and raise your bid. Remember to deal with part-filled orders!
   */
  private void logBuyNotFilledYet() {
    LOG.info(() -> context.getMarketName()
            + " Still have BUY Order! " + lastOrder.id
            + " waiting to fill at [" + lastOrder.price
            + "] - holding last BUY order...");
  }

  /*
   * Could be nobody has jumped on it yet, or the order is only part filled, or market
   * has gone down and we've been undercut and have a stuck sell order. If stuck, we have to
   * wait for market to recover for the order to fill, or you could tweak this code to
   * cancel the current order and lower your ask - remember to deal with any part-filled orders!
   */
  private void logSellOrderNotFilledYet(BigDecimal currentAskPrice) {
    if (currentAskPrice.compareTo(lastOrder.price) < 0) {
      LOG.info(() -> context.getMarketName()
              + " < Current ask price ["
              + currentAskPrice
              + "] is LOWER then last order price ["
              + lastOrder.price
              + "] - holding last SELL order...");

    } else if (currentAskPrice.compareTo(lastOrder.price) > 0) {
      LOG.error(() -> context.getMarketName()
              + " > Current ask price ["
              + currentAskPrice
              + "] is HIGHER than last order price ["
              + lastOrder.price
              + "] - IMPOSSIBLE! BX-bot must have sold?????");

    } else if (currentAskPrice.compareTo(lastOrder.price) == 0) {
      LOG.info(() -> context.getMarketName()
              + " = Current ask price ["
              + currentAskPrice
              + "] is EQUAL to last order price ["
              + lastOrder.price
              + "] - holding last SELL order...");
    }
  }

  /**
   * Your timeout handling code could go here, e.g. you might want to check if the order
   * actually made it to the exchange? And if not, resend it...
   * We are just going to LOG.it and swallow it, and wait for next trade cycle.
   */
  private void handleExchangeNetworkException(String msg, ExchangeNetworkException e) {
    LOG.error(() -> context.getMarketName()
            + " " + msg + " because Exchange threw network "
            + "exception. Waiting until next trade cycle. Last Order: " + lastOrder, e);
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
