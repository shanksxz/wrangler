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
 * Token representing a byte size value with unit (e.g., 10K, 5M, 2G, 1T).
 * Provides methods to convert between different byte units and retrieve
 * the canonical value in bytes.
 */
public class ByteSize implements Token {
  private final long bytes;

  public ByteSize(String bytes) {
    this.bytes = convertToBytes(bytes);
  }

  private long convertToBytes(String bytes) {
    String numericPart = bytes.replaceAll("[^0-9]", "");
    String unitPart = bytes.replaceAll("[0-9]", "").toUpperCase();

    if (numericPart.isEmpty()) {
      throw new IllegalArgumentException("No numeric value found in: " + bytes);
    }
    long value = Long.parseLong(numericPart);
    switch (unitPart) {
      case "B":
        return value;
      case "K":
      case "KB":
        return value * 1024;
      case "M":
      case "MB":
        return value * 1024 * 1024;
      case "G":
      case "GB":
        return value * 1024 * 1024 * 1024;
      case "T":
      case "TB":
        return value * 1024 * 1024 * 1024 * 1024;
      default:
        throw new IllegalArgumentException("Invalid unit: " + unitPart);
    }
  }

  @Override
  public Object value() {
    return bytes;
  }

  @Override
  public TokenType type() {
    return TokenType.BYTE_SIZE;
  }

  @Override
  public JsonElement toJson() {
    JsonObject object = new JsonObject();
    object.addProperty("type", TokenType.BYTE_SIZE.name());
    object.addProperty("value", bytes);
    return object;
  }

  public long getBytes() {
    return bytes;
  }

  public long getKilobytes() {
    return bytes / 1024;
  }

  public long getMegabytes() {
    return bytes / (1024 * 1024);
  }

  public long getGigabytes() {
    return bytes / (1024 * 1024 * 1024);
  }

  public long getTerabytes() {
    return bytes / (1024 * 1024 * 1024 * 1024);
  }

}
