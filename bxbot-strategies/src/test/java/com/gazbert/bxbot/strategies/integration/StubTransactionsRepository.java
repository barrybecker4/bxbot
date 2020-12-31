package com.gazbert.bxbot.strategies.integration;

import com.gazbert.bxbot.domain.transaction.TransactionEntry;
import com.gazbert.bxbot.repository.TransactionsRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Used in simulation to calculate profit after the run is done.
 */
public class StubTransactionsRepository implements TransactionsRepository {

  private List<TransactionEntry> entries = new ArrayList<>();

  /**
   * Save it.
   *
   * @param entity entity
   * @param <S> the sort of entity to save
   * @return the saved entity
   */
  @Override
  public <S extends TransactionEntry> S save(S entity) {
    entries.add(entity);
    return entity;
  }

  @Override
  public <S extends TransactionEntry> Iterable<S> saveAll(Iterable<S> entities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<TransactionEntry> findByType(String type) {
    return null;
  }

  @Override
  public List<TransactionEntry> findByMarket(String market) {
    return null;
  }

  @Override
  public Optional<TransactionEntry> findById(long id) {
    return Optional.empty();
  }


  @Override
  public Optional<TransactionEntry> findById(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean existsById(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterable<TransactionEntry> findAll() {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterable<TransactionEntry> findAllById(Iterable<Long> longs) {
    throw new UnsupportedOperationException();
  }

  @Override
  public long count() {
    return entries.size();
  }

  @Override
  public void deleteById(Long id) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void delete(TransactionEntry entity) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll(Iterable<? extends TransactionEntry> entities) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void deleteAll() {
    throw new UnsupportedOperationException();
  }
}
