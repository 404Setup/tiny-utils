## 2024-06-16 - String.format in loops
**Learning:** Using `String.format("%02x", b)` inside a loop for hex conversion creates massive overhead due to regex parsing and object creation, taking ~4000ms for 100k iterations compared to ~20ms for bitwise operations.
**Action:** Always use bitwise operations and a lookup array (`HEX_ARRAY`) for byte-to-hex conversion in performance-critical code.
