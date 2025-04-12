/*
 * Copyright © 2017-2023 Cask Data, Inc.
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

package io.cdap.wrangler.directives.aggregates;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.List;

/**
 * A directive that aggregates columns containing byte sizes and time durations.
 * This directive processes rows and calculates total or average values for byte
 * size
 * and time duration columns, supporting various output units for the aggregated
 * data.
 */
public class AggregateStats implements Directive {
  public static final String NAME = "aggregate-stats";
  private String byteSizeColumn;
  private String timeDurationColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private String sizeOutputUnit;
  private String timeOutputUnit;
  private String aggregationType;

  private long totalBytes = 0;
  private long totalMilliseconds = 0;
  private long rowCount = 0;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("byteSizeColumn", TokenType.COLUMN_NAME);
    builder.define("timeDurationColumn", TokenType.COLUMN_NAME);
    builder.define("totalSizeColumn", TokenType.COLUMN_NAME);
    builder.define("totalTimeColumn", TokenType.COLUMN_NAME);
    builder.define("sizeOutputUnit", TokenType.TEXT, "MB");
    builder.define("timeOutputUnit", TokenType.TEXT, "s");
    builder.define("aggregationType", TokenType.TEXT, "total");
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.byteSizeColumn = ((ColumnName) args.value("byteSizeColumn")).value();
    this.timeDurationColumn = ((ColumnName) args.value("timeDurationColumn")).value();
    this.totalSizeColumn = ((ColumnName) args.value("totalSizeColumn")).value();
    this.totalTimeColumn = ((ColumnName) args.value("totalTimeColumn")).value();

    this.sizeOutputUnit = "MB";
    this.timeOutputUnit = "s";
    this.aggregationType = "total";

    if (args.contains("sizeOutputUnit")) {
      this.sizeOutputUnit = ((Text) args.value("sizeOutputUnit")).value();
    }
    if (args.contains("timeOutputUnit")) {
      this.timeOutputUnit = ((Text) args.value("timeOutputUnit")).value();
    }
    if (args.contains("aggregationType")) {
      this.aggregationType = ((Text) args.value("aggregationType")).value();
      if (!this.aggregationType.equals("total") && !this.aggregationType.equals("average")) {
        throw new DirectiveParseException(
            "Invalid aggregation type. Must be either 'total' or 'average'.");
      }
    }
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context)
      throws DirectiveExecutionException, ErrorRowException {

    String bytesKey = NAME + ".totalBytes";
    String msKey = NAME + ".totalMilliseconds";
    String countKey = NAME + ".rowCount";

    Long storedBytes = context.getTransientStore().get(bytesKey);
    Long storedMs = context.getTransientStore().get(msKey);
    Long storedCount = context.getTransientStore().get(countKey);

    if (storedBytes != null) {
      totalBytes = storedBytes;
    }
    if (storedMs != null) {
      totalMilliseconds = storedMs;
    }
    if (storedCount != null) {
      rowCount = storedCount;
    }

    for (Row row : rows) {
      int byteSizeIdx = row.find(byteSizeColumn);
      int timeDurationIdx = row.find(timeDurationColumn);

      if (byteSizeIdx != -1) {
        Object byteSizeObj = row.getValue(byteSizeIdx);
        if (byteSizeObj != null) {
          long bytes = 0;
          if (byteSizeObj instanceof ByteSize) {
            bytes = ((ByteSize) byteSizeObj).getBytes();
          } else if (byteSizeObj instanceof String) {
            bytes = new ByteSize((String) byteSizeObj).getBytes();
          } else if (byteSizeObj instanceof Number) {
            bytes = ((Number) byteSizeObj).longValue();
          }

          totalBytes += bytes;
        }
      }

      if (timeDurationIdx != -1) {
        Object timeDurationObj = row.getValue(timeDurationIdx);
        if (timeDurationObj != null) {
          long milliseconds = 0;
          if (timeDurationObj instanceof TimeDuration) {
            milliseconds = ((TimeDuration) timeDurationObj).getMilliseconds();
          } else if (timeDurationObj instanceof String) {
            milliseconds = new TimeDuration((String) timeDurationObj).getMilliseconds();
          } else if (timeDurationObj instanceof Number) {
            milliseconds = ((Number) timeDurationObj).longValue();
          }

          totalMilliseconds += milliseconds;
        }
      }

      rowCount++;
    }

    context.getTransientStore().set(TransientVariableScope.GLOBAL, bytesKey, totalBytes);
    context.getTransientStore().set(TransientVariableScope.GLOBAL, msKey, totalMilliseconds);
    context.getTransientStore().set(TransientVariableScope.GLOBAL, countKey, rowCount);

    if (context.getTransientStore().get("last.batch") != null) {
      Row result = new Row();

      double sizeValue = calculateSizeValue();
      double timeValue = calculateTimeValue();

      result.add(totalSizeColumn, sizeValue);
      result.add(totalTimeColumn, timeValue);

      return List.of(result);
    }

    return List.of();
  }

  private double calculateSizeValue() {
    double bytes = totalBytes;
    if (aggregationType.equals("average") && rowCount > 0) {
      bytes = bytes / rowCount;
    }

    switch (sizeOutputUnit.toUpperCase()) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024.0);
      case "GB":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      default:
        return bytes / (1024.0 * 1024.0);
    }
  }

  private double calculateTimeValue() {
    double milliseconds = totalMilliseconds;
    if (aggregationType.equals("average") && rowCount > 0) {
      milliseconds = milliseconds / rowCount;
    }

    switch (timeOutputUnit.toLowerCase()) {
      case "ms":
        return milliseconds;
      case "s":
        return milliseconds / 1000.0;
      case "m":
      case "min":
        return milliseconds / (1000.0 * 60.0);
      case "h":
        return milliseconds / (1000.0 * 60.0 * 60.0);
      case "d":
        return milliseconds / (1000.0 * 60.0 * 60.0 * 24.0);
      default:
        return milliseconds / 1000.0;
    }
  }

  @Override
  public void destroy() {
  }
}
