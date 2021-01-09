package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.repository.TransactionsRepository;
import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import com.gazbert.bxbot.strategy.api.TradingStrategy;
import com.gazbert.bxbot.trading.api.Market;
import com.gazbert.bxbot.trading.api.TradingApi;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractTradingStrategy implements TradingStrategy {

  protected static final Logger LOG = LogManager.getLogger();

  protected TradingContext context;
  protected BaseStrategyConfig strategyConfig;

  @Autowired
  protected TransactionsRepository transactionRepo;

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
    strategyConfig = createTradingStrategyConfig(config);
    this.transactionRepo = transactionRepo;
    LOG.info(() -> strategyConfig.getStrategyId() + " was initialised successfully!");
  }

  protected abstract BaseStrategyConfig createTradingStrategyConfig(IStrategyConfigItems config);

}
