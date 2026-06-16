## 2024-06-16 - String.format in loops
**Learning:** Using `String.format("%02x", b)` inside a loop for hex conversion creates massive overhead due to regex parsing and object creation, taking ~4000ms for 100k iterations compared to ~20ms for bitwise operations.
**Action:** Always use bitwise operations and a lookup array (`HEX_ARRAY`) for byte-to-hex conversion in performance-critical code.

## 2024-05-18 - UUID Parsing Optimization
**Learning:** Parsing UUIDs using regex and String.format is significantly slower than parsing using Long.parseUnsignedLong and reconstructing the UUID directly. However, we must ensure backward compatibility by safely handling formatting issues without throwing unexpected exceptions when formatting fails.
**Action:** Replace `UUID_PATTERN` matcher and `String.format` with manual parsing of most significant and least significant bits for a measurable performance gain.
