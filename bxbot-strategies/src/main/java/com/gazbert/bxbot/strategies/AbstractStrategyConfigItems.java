package com.gazbert.bxbot.strategies;

import com.gazbert.bxbot.strategy.api.IStrategyConfigItems;
import com.google.common.base.MoreObjects;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates (optional) Strategy Config Items for simulated strategy.
 */
public abstract class AbstractStrategyConfigItems implements IStrategyConfigItems {

  protected String strategyId;
  protected Map<String, String> items;


  public String getStrategyId() {
    return strategyId;
  }

  @Override
  public String getConfigItem(String key) {
    return items.get(key);
  }

  @Override
  public int getNumberOfConfigItems() {
    return items.size();
  }

  @Override
  public Set<String> getConfigItemKeys() {
    return Collections.unmodifiableSet(items.keySet());
  }

  public void setItems(Map<String, String> items) {
    this.items = items;
  }

  Map<String, String> getItems() {
    return items;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
            .add("items", items)
            .toString();
  }
}
