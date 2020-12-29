package com.gazbert.bxbot.strategies;

import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.FILLED;
import static com.gazbert.bxbot.domain.transaction.TransactionEntry.Status.SENT;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import java.util.Optional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Just for testing that the H2 DB works.
 */
public class TempDbDemo {

  private static final Logger LOG = LogManager.getLogger();

  static void demoDb(TransactionsRepository transactionRepo) {
    if (transactionRepo == null) {
      LOG.info(() -> "No TransactionRepo!!");
      return;
    }
    transactionRepo.save(new TransactionEntry("42", "BUY", SENT,
            "Jack Bauer", 0.234, 345.0, "strategy", "bitstamp"));
    transactionRepo.save(new TransactionEntry("27", "SELL", SENT,
            "Cloe O'Brian", 0.123, 2.345, "strategy", "bitstamp"));
    transactionRepo.save(new TransactionEntry("42", "BUY", FILLED,
            "Kim Bauer", 3.456, 234.2, "strategy", "bitstamp"));
    transactionRepo.save(new TransactionEntry("43", "SELL", SENT,
            "David Palmer", 4.567, 3453.3, "strategy", "bitstamp"));
    transactionRepo.save(new TransactionEntry("45", "SELL", FILLED,
            "Michelle Dessler", 5.678, 34534.44, "strategy", "bitstamp"));

    // fetch all transactions
    LOG.info(() -> "Transactions found with findAll():");
    LOG.info(() -> "-------------------------------");
    for (TransactionEntry txn : transactionRepo.findAll()) {
      LOG.info(txn::toString);
    }
    LOG.info(() -> "");

    // fetch an individual customer by ID
    Optional<TransactionEntry> txn = transactionRepo.findById(42L);
    LOG.info(() -> "Transaction found with findById(42L):");
    LOG.info(() -> "--------------------------------");
    LOG.info(txn::toString);
    LOG.info(() -> "");

    // fetch customers by last name
    LOG.info(() -> "Transaction found with findByMarket('Bauer'):");
    LOG.info(() -> "--------------------------------------------");
    transactionRepo.findByMarket("Jack Bauer").forEach(bauer -> {
      LOG.info(bauer::toString);
    });
    LOG.info(() -> "");
  }
}
