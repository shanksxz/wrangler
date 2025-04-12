/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Token representing a time duration value with unit (e.g., 500ms, 10s, 5m, 2h,
 * 1d).
 * Provides methods to convert between different time units and retrieve
 * the canonical value in milliseconds.
 */
public class TimeDuration implements Token {
  private final long milliseconds;

  public TimeDuration(String duration) {
    this.milliseconds = convertToMilliseconds(duration);
  }

  private long convertToMilliseconds(String duration) {
    if (duration.endsWith("ms")) {
      return Long.parseLong(duration.substring(0, duration.length() - 2));
    }
    String unit = duration.substring(duration.length() - 1).toUpperCase();
    long value = Long.parseLong(duration.substring(0, duration.length() - 1));
    switch (unit) {
      case "S":
        return value * 1000;
      case "M":
        return value * 60 * 1000;
      case "H":
        return value * 60 * 60 * 1000;
      case "D":
        return value * 24 * 60 * 60 * 1000;
      default:
        throw new IllegalArgumentException("Invalid unit: " + unit);
    }
  }

  @Override
  public Object value() {
    return milliseconds;
  }

  @Override
  public TokenType type() {
    return TokenType.TIME_DURATION;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.TIME_DURATION.name());
    object.addProperty("value", milliseconds);
    return object;
  }

  public long getMilliseconds() {
    return milliseconds;
  }

  public long getSeconds() {
    return milliseconds / 1000;
  }

  public long getMinutes() {
    return milliseconds / (1000 * 60);
  }

  public long getHours() {
    return milliseconds / (1000 * 60 * 60);
  }

  public long getDays() {
    return milliseconds / (1000 * 60 * 60 * 24);
  }
}
