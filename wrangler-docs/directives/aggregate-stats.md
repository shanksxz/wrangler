# Aggregate Stats

The `aggregate-stats` directive performs aggregation operations on columns containing byte sizes and time durations, calculating total or average values with flexible output unit options.

## Syntax

```
aggregate-stats :byteSizeColumn :timeDurationColumn totalSizeColumn totalTimeColumn [sizeOutputUnit] [timeOutputUnit] [aggregationType]
```

## Arguments

* **byteSizeColumn**: Column containing byte size values (can be ByteSize objects, strings like "10KB", or numeric values).
* **timeDurationColumn**: Column containing time duration values (can be TimeDuration objects, strings like "100ms", or numeric values).
* **totalSizeColumn**: Name of the output column to store aggregated size values.
* **totalTimeColumn**: Name of the output column to store aggregated time values.
* **sizeOutputUnit** (optional): Unit for size output (B, KB, MB, GB, TB). Default is MB.
* **timeOutputUnit** (optional): Unit for time output (ms, s, m/min, h, d). Default is s (seconds).
* **aggregationType** (optional): Aggregation type to perform: "total" or "average". Default is "total".

## Usage Notes

* This directive aggregates values across all input rows and outputs a single row with the aggregated results.
* For byte size aggregation, values are converted to bytes internally before aggregation.
* For time duration aggregation, values are converted to milliseconds internally before aggregation.
* The directive automatically handles different input formats:
  * Byte sizes can be ByteSize objects, strings with units (e.g., "10KB"), or raw numbers (interpreted as bytes).
  * Time durations can be TimeDuration objects, strings with units (e.g., "100ms"), or raw numbers (interpreted as milliseconds).
* Using the "average" aggregation type will divide the total by the number of rows processed.
* The results are output in the specified units (or default units if not specified).

## Example 1: Total Aggregation

Using these records:

| data_size | response_time |
|-----------|---------------|
| 10KB      | 100ms         |
| 1.5MB     | 2.5s          |
| 500KB     | 750ms         |

After applying:

```
parse-as-byte-size :data_size
parse-as-time-duration :response_time
aggregate-stats :data_size :response_time total_size total_time
```

The output is a single row:

| total_size | total_time |
|------------|------------|
| 2.0       | 3.35       |

Where total_size is in MB (default unit) and total_time is in seconds (default unit).

## Example 2: Average with Custom Units

Using these records:

| data_size | response_time |
|-----------|---------------|
| 10KB      | 100ms         |
| 1.5MB     | 2.5s          |
| 500KB     | 750ms         |

After applying:

```
parse-as-byte-size :data_size
parse-as-time-duration :response_time
aggregate-stats :data_size :response_time avg_size avg_time KB ms average
```

The output is a single row:

| avg_size | avg_time |
|----------|----------|
| 669.33   | 1116.67  |

Where avg_size is in KB and avg_time is in ms, and values are averaged across the 3 input rows.

## Related Directives

* [parse-as-byte-size](parse-as-byte-size.md) - Parses strings as byte sizes
* [parse-as-time-duration](parse-as-time-duration.md) - Parses strings as time durations 