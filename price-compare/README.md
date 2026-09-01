# Taiwan Price Compare — Kotlin Port

Kotlin port of [`coseto6125/mcp-taiwan-price-compare`](https://github.com/coseto6125/mcp-taiwan-price-compare), preserving its MIT licensing terms. This project is a standalone, JVM-first comparison engine; an Android app can consume the same core classes without running Python or an MCP process.

The port keeps the upstream model: all-platform concurrent fan-out, a per-platform timeout, common price and keyword filtering, deterministic local price sorting, and platform parsers kept separate from the shared pipeline.

