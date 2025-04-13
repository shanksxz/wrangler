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

package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests {@link AggregateStats} directive.
 */
public class AggregateStatsTest {

  @Mock
  private Arguments arguments;

  @Mock
  private ExecutorContext context;

  @Mock
  private TransientStore transientStore;

  private AggregateStats directive;
  private List<Row> rows;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    directive = new AggregateStats();
    rows = new ArrayList<>();

    Mockito.when(context.getTransientStore()).thenReturn(transientStore);
  }

  @Test
  public void testDefine() {
    Assert.assertEquals("aggregate-stats", directive.define().getDirectiveName());
  }

  @Test
  public void testInitialize() throws DirectiveParseException {
    ColumnName byteSizeColumn = new ColumnName("size");
    ColumnName timeDurationColumn = new ColumnName("time");
    ColumnName totalSizeColumn = new ColumnName("total_size");
    ColumnName totalTimeColumn = new ColumnName("total_time");
    Text sizeOutputUnit = new Text("MB");
    Text timeOutputUnit = new Text("s");
    Text aggregationType = new Text("total");

    Mockito.when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
    Mockito.when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
    Mockito.when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
    Mockito.when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
    Mockito.when(arguments.contains("sizeOutputUnit")).thenReturn(true);
    Mockito.when(arguments.value("sizeOutputUnit")).thenReturn(sizeOutputUnit);
    Mockito.when(arguments.contains("timeOutputUnit")).thenReturn(true);
    Mockito.when(arguments.value("timeOutputUnit")).thenReturn(timeOutputUnit);
    Mockito.when(arguments.contains("aggregationType")).thenReturn(true);
    Mockito.when(arguments.value("aggregationType")).thenReturn(aggregationType);

    directive.initialize(arguments);
  }

  @Test
  public void testExecuteWithByteSize() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirective();
    Row row1 = new Row();
    row1.add("size", new ByteSize("1MB"));
    row1.add("time", new TimeDuration("1s"));

    Row row2 = new Row();
    row2.add("size", new ByteSize("2MB"));
    row2.add("time", new TimeDuration("2s"));

    rows.add(row1);
    rows.add(row2);

    List<Row> result = directive.execute(rows, context);

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalBytes"),
        ArgumentMatchers.eq(3L * 1024 * 1024));

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalMilliseconds"),
        ArgumentMatchers.eq(3000L));

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.rowCount"),
        ArgumentMatchers.eq(2L));

    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testExecuteWithNumericValues()
      throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirective();

    Row row1 = new Row();
    row1.add("size", 1024L);
    row1.add("time", 1000L);

    Row row2 = new Row();
    row2.add("size", 2048L);
    row2.add("time", 2000L);

    rows.add(row1);
    rows.add(row2);

    List<Row> result = directive.execute(rows, context);

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalBytes"),
        ArgumentMatchers.eq(3072L));

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalMilliseconds"),
        ArgumentMatchers.eq(3000L));

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.rowCount"),
        ArgumentMatchers.eq(2L));

    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testExecuteWithStringValues()
      throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirective();

    Row row1 = new Row();
    row1.add("size", "1KB");
    row1.add("time", "1s");

    Row row2 = new Row();
    row2.add("size", "2KB");
    row2.add("time", "2s");

    rows.add(row1);
    rows.add(row2);

    List<Row> result = directive.execute(rows, context);

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalBytes"),
        ArgumentMatchers.eq(3L * 1024)); // 3KB in bytes

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.totalMilliseconds"),
        ArgumentMatchers.eq(3000L)); // 3s in ms

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.rowCount"),
        ArgumentMatchers.eq(2L));

    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testExecuteLastBatch() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirective();

    Mockito.when(transientStore.get("last.batch")).thenReturn(true);

    Mockito.when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024); // 3MB
    Mockito.when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L); // 3s
    Mockito.when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);

    List<Row> result = directive.execute(new ArrayList<>(), context);

    Assert.assertEquals(1, result.size());
    Row resultRow = result.get(0);

    Assert.assertEquals(3.0, ((Double) resultRow.getValue("total_size")).doubleValue(), 0.01);
    Assert.assertEquals(3.0, ((Double) resultRow.getValue("total_time")).doubleValue(), 0.01);
  }

  @Test
  public void testExecuteLastBatchWithAverage()
      throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirectiveWithAverage();

    Mockito.when(transientStore.get("last.batch")).thenReturn(true);

    Mockito.when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024); // 3MB
    Mockito.when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L); // 3s
    Mockito.when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);

    List<Row> result = directive.execute(new ArrayList<>(), context);

    Assert.assertEquals(1, result.size());
    Row resultRow = result.get(0);

    Assert.assertEquals(1.0, ((Double) resultRow.getValue("total_size")).doubleValue(), 0.01);
    Assert.assertEquals(1.0, ((Double) resultRow.getValue("total_time")).doubleValue(), 0.01);
  }

  @Test
  public void testExecuteWithNullValues()
      throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirective();

    Row row1 = new Row();
    row1.add("size", null);
    row1.add("time", null);

    Row row2 = new Row();
    row2.add("other_col", "value");

    rows.add(row1);
    rows.add(row2);

    List<Row> result = directive.execute(rows, context);

    Mockito.verify(transientStore).set(ArgumentMatchers.eq(TransientVariableScope.GLOBAL),
        ArgumentMatchers.eq("aggregate-stats.rowCount"),
        ArgumentMatchers.eq(2L));

    Assert.assertEquals(0, result.size());
  }

  @Test
  public void testExecuteWithVariousOutputUnits()
      throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
    initializeDirectiveWithCustomUnits("KB", "ms");

    Mockito.when(transientStore.get("last.batch")).thenReturn(true);

    Mockito.when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024);
    Mockito.when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L);
    Mockito.when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);

    List<Row> result = directive.execute(new ArrayList<>(), context);

    Assert.assertEquals(3072.0, ((Double) result.get(0).getValue("total_size")).doubleValue(), 0.01);
    Assert.assertEquals(3000.0, ((Double) result.get(0).getValue("total_time")).doubleValue(), 0.01);
  }

  private void initializeDirective() throws DirectiveParseException {
    ColumnName byteSizeColumn = new ColumnName("size");
    ColumnName timeDurationColumn = new ColumnName("time");
    ColumnName totalSizeColumn = new ColumnName("total_size");
    ColumnName totalTimeColumn = new ColumnName("total_time");

    Mockito.when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
    Mockito.when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
    Mockito.when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
    Mockito.when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
    Mockito.when(arguments.contains("sizeOutputUnit")).thenReturn(false);
    Mockito.when(arguments.contains("timeOutputUnit")).thenReturn(false);
    Mockito.when(arguments.contains("aggregationType")).thenReturn(false);

    directive.initialize(arguments);
  }

  private void initializeDirectiveWithAverage() throws DirectiveParseException {
    ColumnName byteSizeColumn = new ColumnName("size");
    ColumnName timeDurationColumn = new ColumnName("time");
    ColumnName totalSizeColumn = new ColumnName("total_size");
    ColumnName totalTimeColumn = new ColumnName("total_time");
    Text aggregationType = new Text("average");

    Mockito.when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
    Mockito.when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
    Mockito.when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
    Mockito.when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
    Mockito.when(arguments.contains("sizeOutputUnit")).thenReturn(false);
    Mockito.when(arguments.contains("timeOutputUnit")).thenReturn(false);
    Mockito.when(arguments.contains("aggregationType")).thenReturn(true);
    Mockito.when(arguments.value("aggregationType")).thenReturn(aggregationType);

    directive.initialize(arguments);
  }

  private void initializeDirectiveWithCustomUnits(String sizeUnit, String timeUnit) throws DirectiveParseException {
    ColumnName byteSizeColumn = new ColumnName("size");
    ColumnName timeDurationColumn = new ColumnName("time");
    ColumnName totalSizeColumn = new ColumnName("total_size");
    ColumnName totalTimeColumn = new ColumnName("total_time");
    Text sizeOutputUnit = new Text(sizeUnit);
    Text timeOutputUnit = new Text(timeUnit);

    Mockito.when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
    Mockito.when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
    Mockito.when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
    Mockito.when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
    Mockito.when(arguments.contains("sizeOutputUnit")).thenReturn(true);
    Mockito.when(arguments.value("sizeOutputUnit")).thenReturn(sizeOutputUnit);
    Mockito.when(arguments.contains("timeOutputUnit")).thenReturn(true);
    Mockito.when(arguments.value("timeOutputUnit")).thenReturn(timeOutputUnit);
    Mockito.when(arguments.contains("aggregationType")).thenReturn(false);

    directive.initialize(arguments);
  }
}
