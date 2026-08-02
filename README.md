# Skyflow Java

This repository hosts Skyflow's Java SDKs for integrating Skyflow into a Java backend. It's a single Maven reactor with more than one published artifact — pick the package that matches what you need below.

[![CI](https://img.shields.io/static/v1?label=CI&message=passing&color=green?style=plastic&logo=github)](https://github.com/skyflowapi/skyflow-java/actions)
[![GitHub release](https://img.shields.io/github/v/release/skyflowapi/skyflow-java.svg)](https://github.com/skyflowapi/skyflow-java/releases)
[![License](https://img.shields.io/github/license/skyflowapi/skyflow-java)](https://github.com/skyflowapi/skyflow-java/blob/main/LICENSE)

## Which package do I want?

| Package | Artifact | README | Vault Type | Supported Operations |
|---|---|---|---|---|
| **skyvault** | `com.skyflow:skyflow-java` | [skyvault/README.md](skyvault/README.md) | Privacy DB | <ul><li>Insert</li><li>Detokenize</li><li>Tokenize</li><li>Get</li><li>Update</li><li>Delete</li><li>Query</li><li>Upload File</li><li>Detect (deidentify text/file, reidentify text)</li><li>Connections (invoke third-party connections)</li></ul> |
| **flowvault** | `com.skyflow:skyflow-flowvault-java` | [flowvault/README.md](flowvault/README.md) | Flow DB | <ul><li>Bulk Insert</li><li>Bulk Tokenize</li><li>Bulk Detokenize</li><li>Bulk Delete Tokens</li></ul>(each with a sync and async variant) |

`flowvault` shares auth/client setup with `skyvault` — both depend on the `common` module.

> Migrating from v1? See skyvault's **[Migration Guide](docs/migrate_to_v2.md)**. V1 is in maintenance mode and will reach End of Life on October 31, 2026.

## Repository layout

The root `pom.xml` (`packaging=pom`) aggregates this Maven reactor:

- `common/` — shared client, credentials, config, and error-handling code used by both `skyvault` and `flowvault`
- `skyvault/` — the `skyflow-java` SDK ([README](skyvault/README.md))
- `flowvault/` — the `skyflow-flowvault-java` SDK ([README](flowvault/README.md))

`v3/` is a separate, standalone Maven project outside this reactor.

## Documentation

- [skyvault API Reference](docs/api_reference.md) — full list of request builder methods, response getters, enums, and service-account utilities
- [Migrate from v1 to v2](docs/migrate_to_v2.md)

## Reporting a Vulnerability

If you discover a potential security issue in this project, please reach out to us at **security@skyflow.com**. Please do not create public GitHub issues or Pull Requests, as malicious actors could potentially view them.
