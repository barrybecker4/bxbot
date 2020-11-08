/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Gareth Jon Lynch
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.gazbert.bxbot.core.config.exchange;

import com.gazbert.bxbot.exchange.api.AuthenticationConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * Exchange API Authentication config.
 *
 * @author gazbert
 */
public class AuthenticationConfigImpl implements AuthenticationConfig {

  private Map<String, String> items;

  public AuthenticationConfigImpl() {
    items = new HashMap<>();
  }

  /**
   * If name is "client-id", "key" or "secret" respectively, then we will attempt to get the
   * values from EXCHANGE_CLIENT_ID, EXCHANGE_KEY or EXCHANGE_SECRET respectively so that
   * these values do not need to be stored in the yaml files.
   * @param name the name of the item to fetch.
   * @return the values for the specified name
   */
  @Override
  public String getItem(String name) {
    if (!items.containsKey(name)) {
      if ("key".equals(name)) {
        return System.getenv("EXCHANGE_KEY");
      } else if ("secret".equals(name)) {
        return System.getenv("EXCHANGE_SECRET");
      } else if ("client-id".equals(name)) {
        return System.getenv("EXCHANGE_CLIENT_ID");
      }
    }
    return items.get(name);
  }

  Map<String, String> getItems() {
    return items;
  }

  public void setItems(Map<String, String> items) {
    this.items = items;
  }
}
