package io.cdap.directives.aggregates;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

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
import io.cdap.wrangler.directives.aggregates.AggregateStats;

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
        
        when(context.getTransientStore()).thenReturn(transientStore);
    }

    @Test
    public void testDefine() {
        assertEquals("aggregate-stats", directive.define().getDirectiveName());
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
        
        when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
        when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
        when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
        when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
        when(arguments.contains("sizeOutputUnit")).thenReturn(true);
        when(arguments.value("sizeOutputUnit")).thenReturn(sizeOutputUnit);
        when(arguments.contains("timeOutputUnit")).thenReturn(true);
        when(arguments.value("timeOutputUnit")).thenReturn(timeOutputUnit);
        when(arguments.contains("aggregationType")).thenReturn(true);
        when(arguments.value("aggregationType")).thenReturn(aggregationType);
        
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
        
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalBytes"), 
                                   eq(3L * 1024 * 1024)); 
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalMilliseconds"), 
                                   eq(3000L)); 
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.rowCount"), 
                                   eq(2L));
                                   
        assertEquals(0, result.size());
    }

    @Test
    public void testExecuteWithNumericValues() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
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
        
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalBytes"), 
                                   eq(3072L));
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalMilliseconds"), 
                                   eq(3000L));
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.rowCount"), 
                                   eq(2L));
                                   
        assertEquals(0, result.size());
    }

    @Test
    public void testExecuteWithStringValues() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
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
        
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalBytes"), 
                                   eq(3L * 1024)); // 3KB in bytes
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.totalMilliseconds"), 
                                   eq(3000L)); // 3s in ms
                                   
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.rowCount"), 
                                   eq(2L));
                                   
        assertEquals(0, result.size());
    }

    @Test
    public void testExecuteLastBatch() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
        initializeDirective();
        
        when(transientStore.get("last.batch")).thenReturn(true);
        
        when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024); // 3MB
        when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L);  // 3s
        when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);
        
        List<Row> result = directive.execute(new ArrayList<>(), context);
        
        assertEquals(1, result.size());
        Row resultRow = result.get(0);
        
        assertEquals(3.0, ((Double) resultRow.getValue("total_size")).doubleValue(), 0.01);
        assertEquals(3.0, ((Double) resultRow.getValue("total_time")).doubleValue(), 0.01);
    }

    @Test
    public void testExecuteLastBatchWithAverage() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
        initializeDirectiveWithAverage();
        
        when(transientStore.get("last.batch")).thenReturn(true);
        
        when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024); // 3MB
        when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L);  // 3s
        when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);
        
        List<Row> result = directive.execute(new ArrayList<>(), context);
        
        assertEquals(1, result.size());
        Row resultRow = result.get(0);
        
        assertEquals(1.0, ((Double) resultRow.getValue("total_size")).doubleValue(), 0.01);
        assertEquals(1.0, ((Double) resultRow.getValue("total_time")).doubleValue(), 0.01);
    }

    @Test
    public void testExecuteWithNullValues() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
        initializeDirective();
        
        Row row1 = new Row();
        row1.add("size", null);
        row1.add("time", null);
        
        Row row2 = new Row();
        row2.add("other_col", "value");
        
        rows.add(row1);
        rows.add(row2);
        
        List<Row> result = directive.execute(rows, context);
        
        verify(transientStore).set(eq(TransientVariableScope.GLOBAL), 
                                   eq("aggregate-stats.rowCount"), 
                                   eq(2L));
        
        assertEquals(0, result.size());
    }

    @Test
    public void testExecuteWithVariousOutputUnits() throws DirectiveExecutionException, ErrorRowException, DirectiveParseException {
        initializeDirectiveWithCustomUnits("KB", "ms");
        
        when(transientStore.get("last.batch")).thenReturn(true);
        
        when(transientStore.get("aggregate-stats.totalBytes")).thenReturn(3L * 1024 * 1024);
        when(transientStore.get("aggregate-stats.totalMilliseconds")).thenReturn(3000L);
        when(transientStore.get("aggregate-stats.rowCount")).thenReturn(3L);
        
        List<Row> result = directive.execute(new ArrayList<>(), context);
        
        assertEquals(3072.0, ((Double) result.get(0).getValue("total_size")).doubleValue(), 0.01);
        assertEquals(3000.0, ((Double) result.get(0).getValue("total_time")).doubleValue(), 0.01);
    }

    private void initializeDirective() throws DirectiveParseException {
        ColumnName byteSizeColumn = new ColumnName("size");
        ColumnName timeDurationColumn = new ColumnName("time");
        ColumnName totalSizeColumn = new ColumnName("total_size");
        ColumnName totalTimeColumn = new ColumnName("total_time");
        
        when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
        when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
        when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
        when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
        when(arguments.contains("sizeOutputUnit")).thenReturn(false);
        when(arguments.contains("timeOutputUnit")).thenReturn(false);
        when(arguments.contains("aggregationType")).thenReturn(false);
        
        directive.initialize(arguments);
    }
    
    private void initializeDirectiveWithAverage() throws DirectiveParseException {
        ColumnName byteSizeColumn = new ColumnName("size");
        ColumnName timeDurationColumn = new ColumnName("time");
        ColumnName totalSizeColumn = new ColumnName("total_size");
        ColumnName totalTimeColumn = new ColumnName("total_time");
        Text aggregationType = new Text("average");
        
        when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
        when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
        when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
        when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
        when(arguments.contains("sizeOutputUnit")).thenReturn(false);
        when(arguments.contains("timeOutputUnit")).thenReturn(false);
        when(arguments.contains("aggregationType")).thenReturn(true);
        when(arguments.value("aggregationType")).thenReturn(aggregationType);
        
        directive.initialize(arguments);
    }
    
    private void initializeDirectiveWithCustomUnits(String sizeUnit, String timeUnit) throws DirectiveParseException {
        ColumnName byteSizeColumn = new ColumnName("size");
        ColumnName timeDurationColumn = new ColumnName("time");
        ColumnName totalSizeColumn = new ColumnName("total_size");
        ColumnName totalTimeColumn = new ColumnName("total_time");
        Text sizeOutputUnit = new Text(sizeUnit);
        Text timeOutputUnit = new Text(timeUnit);
        
        when(arguments.value("byteSizeColumn")).thenReturn(byteSizeColumn);
        when(arguments.value("timeDurationColumn")).thenReturn(timeDurationColumn);
        when(arguments.value("totalSizeColumn")).thenReturn(totalSizeColumn);
        when(arguments.value("totalTimeColumn")).thenReturn(totalTimeColumn);
        when(arguments.contains("sizeOutputUnit")).thenReturn(true);
        when(arguments.value("sizeOutputUnit")).thenReturn(sizeOutputUnit);
        when(arguments.contains("timeOutputUnit")).thenReturn(true);
        when(arguments.value("timeOutputUnit")).thenReturn(timeOutputUnit);
        when(arguments.contains("aggregationType")).thenReturn(false);
        
        directive.initialize(arguments);
    }
}
