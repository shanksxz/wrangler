# Parse as Time Duration

The `parse-as-time-duration` directive parses string values representing time durations (such as "100ms", "2.5s", "1h", etc.) into a standardized internal time duration representation.

## Syntax

```
parse-as-time-duration :column
```

## Usage Notes

* This directive converts string representations of time durations into a standardized TimeDuration object.
* Supported units include:
  * ms (milliseconds)
  * s (seconds)
  * m or min (minutes)
  * h (hours)
  * d (days)
* If no unit is specified, the value is interpreted as milliseconds.
* Case is not significant for the units (e.g., "MS" is the same as "ms").
* Spaces between the number and unit are allowed (e.g., "10 ms" is valid).
* Decimal values are supported (e.g., "1.5s").
* The transformation uses standard time conversions (1s = 1000ms, 1m = 60s, etc.).

## Example

Using this record:

| duration_string |
|----------------|
| 100ms          |
| 1.5s           |
| 2 m            |
| 500            |

After applying this directive:

```
parse-as-time-duration :duration_string
```

The column values are converted to TimeDuration objects with standardized millisecond values:

| duration_string |
|----------------|
| TimeDuration(100ms) = 100 milliseconds |
| TimeDuration(1.5s) = 1,500 milliseconds |
| TimeDuration(2m) = 120,000 milliseconds |
| TimeDuration(500) = 500 milliseconds |

## Related Directives

* [aggregate-stats](aggregate-stats.md) - Uses TimeDuration objects for aggregating time values 