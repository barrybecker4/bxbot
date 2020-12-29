package com.gazbert.bxbot.strategies;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.FILLED;
import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
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
import java.util.Stack;
import java.util.stream.Collectors;

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
@Component("barrysMultiOrderTradingStrategy") // for Spring bean injection
public class BarrysMultiOrderTradingStrategy implements TradingStrategy {

  private static final Logger LOG = LogManager.getLogger();
  private static final BigDecimal ONE = new BigDecimal("1");

  private TradingContext context;
  private BarrysMultiOrderTradingStrategyConfig strategyConfig;
  private OrderState lastOrder;

  private final Stack<OrderState> buyOrderStack = new Stack<>();
  private final Stack<OrderState> sellOrderStack = new Stack<>();

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
  public void init(TradingApi tradingApi, Market market, IStrategyConfigItems config) {
    init(new TradingContext(tradingApi, market), config, transactionRepo);
  }

  /**
   * Used by tests.
   */
  public void init(TradingContext context, IStrategyConfigItems config,
                   TransactionsRepository transactionRepo) {
    this.context = context;
    strategyConfig = new BarrysMultiOrderTradingStrategyConfig(config);
    this.transactionRepo = transactionRepo;
    LOG.info(() -> strategyConfig.getStrategyId() + " was initialised successfully!");
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
      LOG.info(() -> "No buy or sell orders (maybe market is closed)");
      return;
    }

    // Get the current BID and ASK spot prices.
    final BigDecimal currentBidPrice = buyOrders.get(0).getPrice();
    final BigDecimal currentAskPrice = sellOrders.get(0).getPrice();
    logPrices(currentBidPrice, currentAskPrice);

    if (lastOrder == null) {
      sendInitialBuyOrder(currentAskPrice);
      return;
    }

    if (!buyOrderStack.isEmpty()) {
      sendSellOrderIfFilledBuyOrder();
    }
    if (!sellOrderStack.isEmpty()) {
      checkForFilledSellOrder(currentBidPrice);
    }
    sendBuyOrderIfSufficientlyLow(currentBidPrice);
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

  /**
   * Algo for executing when the Trading Strategy is invoked for the first time. We start off with
   * a buy order at current BID price.
   * Send buy order at the current strike price. Push that buy order onto a buyOrderStack.
   * (It should fill quickly, but might get stuck if price goes up quickly)
   *
   * @param currentAskPrice the current market ASK price. Use the ask price to increase
   *                        the likelihood that it goes through.
   */
  private void sendInitialBuyOrder(BigDecimal currentAskPrice)
      throws StrategyException {
    LOG.info(() -> context.getMarketName()
                + "Just starting - placing new BUY order at ["
                + PriceUtil.formatPrice(currentAskPrice) + "]");
    sendBuyOrder(currentAskPrice);
  }

  /**
   * If the top order has been filled {
   *    pop the top filled buy order from the buyOrderStack.
   *    set lastTransactionPrice to the price that it was bought at.
   *    send a sell order for lastTransactionPrice * (1 + percent-change-threshold)
   *    push that sell order on a sellOrderStack.
   *    (it will not fill until market goes up by percent-change-threshold)
   * }
   */
  private void sendSellOrderIfFilledBuyOrder() throws StrategyException {
    try {
      boolean lastOrderFound = context.isOrderOpen(buyOrderStack.peek().id);

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        lastOrder = buyOrderStack.pop();
        LOG.info(() -> context.getMarketName()
                + " ^^^ Yay!!! BUY Order Id [" + lastOrder.id + "] filled at ["
                + lastOrder.price + "]");
        persistTransaction(FILLED, lastOrder);

        /*
         * The last buy order was filled, so lets now a new sell order.
         * IMPORTANT - new sell order ASK price must be > (last order price + exchange fees)
         *             because:
         * 1. If we put sell amount in as same amount as previous buy, the exchange barfs if
         *    we don't have enough units to cover the transaction fee.
         * 2. We could end up selling at a loss.
         */
        final BigDecimal amountToAdd =
                lastOrder.price.multiply(strategyConfig.getPercentChangeThreshold());
        LOG.info(() -> context.getMarketName()
                + " Amount to add to last buy order fill price: " + amountToAdd);

        // Most exchanges (if not all) use 8 decimal places.
        // It's usually best to round up the ASK price in your calculations to maximise gains.
        final BigDecimal newAskPrice =
                lastOrder.price.add(amountToAdd).setScale(8, RoundingMode.HALF_UP);

        OrderState newOrder = context.sendSellOrder(lastOrder.amount, newAskPrice);
        persistTransaction(SENT, newOrder);
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
   * If the top order has been filled {
   *    pop the top filled sell order from the sellOrderStack.
   *    set lastTransactionPrice to the price that it was sold at.
   * }
   */
  private void checkForFilledSellOrder(BigDecimal currentBidPrice) throws StrategyException {
    try {
      boolean lastOrderFound = context.isOrderOpen(sellOrderStack.peek().id);

      // If the order is not there, it must have all filled.
      if (!lastOrderFound) {
        lastOrder = sellOrderStack.pop();
        LOG.info(() -> context.getMarketName()
                + " ^^^ Yay!!! SELL Order Id [" + lastOrder.id + "] filled at ["
                + lastOrder.price + "]");
        persistTransaction(FILLED, lastOrder);
      } else {
        logSellOrderNotFilledYet(currentBidPrice);
      }
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("New Order to SELL base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("New order to SELL base currency failed", e);
    }
  }

  /**
   * if currentPrice < lastTransactionPrice * (1 - percent-change-threshold)
   *    and sellStack.size < max-concurrent-sell-orders {
   *        then send buy order at the current bid price. Push that buy order in a buyOrderStack.
   *        set lastTransactionPrice to the current price.
   * }
   *
   * @param currentBidPrice use BID price here to try and get a bit lower than ask.
   *                        This cold be a little risky because it may not fill.
   */
  private void sendBuyOrderIfSufficientlyLow(BigDecimal currentBidPrice)
          throws StrategyException {
    BigDecimal percentThresh = strategyConfig.getPercentChangeThreshold();
    boolean belowThresh =
            currentBidPrice.compareTo(lastOrder.price.multiply(ONE.subtract(percentThresh))) < 0;
    boolean availableSlots = sellOrderStack.size() < strategyConfig.getMaxConcurrentSellOrders();
    if (belowThresh && availableSlots) {
      LOG.info(() -> context.getMarketName()
              + "Placing new BUY order at ["
              + PriceUtil.formatPrice(currentBidPrice) + "]");
      sendBuyOrder(currentBidPrice);
    }
  }

  private void sendBuyOrder(BigDecimal price) throws StrategyException {
    try {
      // Calculate amount of base currency (BTC) to buy for given amount of counter currency (USD).
      final BigDecimal amountOfBaseCurrencyToBuy =
              context.getAmountOfBaseCurrencyToBuy(
                      strategyConfig.getCounterCurrencyBuyOrderAmount());

      lastOrder = context.sendBuyOrder(amountOfBaseCurrencyToBuy, price);
      buyOrderStack.push(lastOrder);

      persistTransaction(SENT, lastOrder);
    } catch (ExchangeNetworkException e) {
      handleExchangeNetworkException("Attempt to BUY base currency failed", e);
    } catch (TradingApiException e) {
      handleTradingApiException("Attempt to BUY base currency failed", e);
    }
  }

  /*
   * Show current open buy orders.
   */
  private void logBuyNotFilledYet() {
    LOG.info(() -> context.getMarketName()
            + " Still have BUY Orders! " + lastOrder.id
            + " They are " + getOrderPrices(buyOrderStack));
  }

  /*
   * Show current open sell orders.
   */
  private void logSellOrderNotFilledYet(BigDecimal currentBidPrice) {
    String prices = getOrderPrices(sellOrderStack);
    if (currentBidPrice.compareTo(lastOrder.price) <= 0) {
      LOG.info(() -> context.getMarketName()
              + " < Current ask price ["
              + currentBidPrice
              + "] is LOWER then last sell order prices: " + prices);

    } else {
      LOG.error(() -> context.getMarketName()
              + " > Current ask price ["
              + currentBidPrice
              + "] is HIGHER than last sell order prices: " + prices
              + " - IMPOSSIBLE! BX-bot must have sold!???");
    }
  }

  private String getOrderPrices(Stack<OrderState> orderStack) {
    return orderStack.stream()
            .map(orderState -> orderState.price.toString())
            .collect(Collectors.joining(", "));
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
