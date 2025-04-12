# Parse as Byte Size

The `parse-as-byte-size` directive parses string values representing byte sizes (such as "10KB", "1.5MB", etc.) into a standardized internal byte size representation.

## Syntax

```
parse-as-byte-size :column
```

## Usage Notes

* This directive converts string representations of byte sizes into a standardized ByteSize object.
* Supported units include B (bytes), KB (kilobytes), MB (megabytes), GB (gigabytes), and TB (terabytes).
* If no unit is specified, the value is interpreted as bytes.
* Case is not significant for the units (e.g., "kb" is the same as "KB").
* Spaces between the number and unit are allowed (e.g., "10 KB" is valid).
* Decimal values are supported (e.g., "1.5MB").
* The transformation uses the standard 1024-based conversion (1KB = 1024B, 1MB = 1024KB, etc.).

## Example

Using this record:

| size_string |
|-------------|
| 10KB        |
| 1.5MB       |
| 2 GB        |
| 512         |

After applying this directive:

```
parse-as-byte-size :size_string
```

The column values are converted to ByteSize objects with standardized byte counts:

| size_string |
|-------------|
| ByteSize(10KB) = 10,240 bytes |
| ByteSize(1.5MB) = 1,572,864 bytes |
| ByteSize(2GB) = 2,147,483,648 bytes |
| ByteSize(512) = 512 bytes |

## Related Directives

* [aggregate-stats](aggregate-stats.md) - Uses ByteSize objects for aggregating size values 