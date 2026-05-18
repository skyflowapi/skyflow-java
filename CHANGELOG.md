# Changelog
All notable changes to this project will be documented in this file.

## [2.0.4] - 2025-05-15
### Changed
- Credential JSON field names `clientID`, `keyID`, `tokenURI` renamed to `clientId`, `keyId`, `tokenUri` (Java camelCase convention). Both old and new forms permanently accepted — no migration required.
- Response maps now return `skyflowId` (camelCase) for Get and Query operations. Legacy `skyflow_id` key retained alongside for backward compatibility; deprecated and will be removed in an upcoming release.
- `GetRequest` and `DetokenizeRequest`: added `downloadUrl()` / `getDownloadUrl()` methods following acronym-as-word convention. Old `downloadURL()` / `getDownloadURL()` kept as `@Deprecated` delegates.
- `QueryResponse`: added `getErrors()` accessor (was missing; all other response classes already had it).
- Removed SDK-level null/empty field value validation from Insert and Update — backend is authoritative per API spec (`additionalProperties: Any type`).

## [2.0.3] - 2025-04-01
### Added
- Initial stable v2 release with builder pattern for all request types.
- Multi-vault support via `Skyflow.builder().addVaultConfig()`.
- Per-client log level configuration.
- Service account authentication: bearer token and signed data token generation.
- Vault operations: Insert, Get, Update, Delete, Query, Tokenize, Detokenize, File Upload.
- Detect API: Deidentify/Reidentify text and file.
- Connections: Invoke connection.

## [1.15.0] - 2024-08-01
### Added
- insert data using bulk operation `insertBulk`

## [1.14.0] - 2024-02-01
### Fixed
- handling of detokenize response to avoid breaking changes.

## [1.13.0] - 2024-01-10
### Added
- Continue on error support for batch Insert.

## [1.12.1] - 2023-11-09
### Fixed
- Static Bearer token being used for multiple Skyflow Client instances.

## [1.12.0] - 2023-10-25
### Added
- `tokens` support in Get Method

## [1.11.0] - 2023-09-01
### Added
- `query` vault API

## [1.10.0] - 2023-08-09
- Added `delete` vault API support. 
## [1.9.0] - 2023-06-08
### Added
- `redaction` key for detokenize method for column group support.

## [1.8.2] - 2023-03-20
### Fixed
- removed grace period logic for bearer token generation.

## [1.8.1] - 2023-03-01
### Fixed
- java cached token bug

## [1.8.0] - 2023-01-10
### Added
- `update` vault API
- `get` vault API

## [1.7.1] - 2022-11-29
### Changed
- `setContext` to `setCtx` method.
- `setTimetoLive` accepts seconds in `Integer` instead of `Double`.

## [1.7.0] - 2022-11-22
### Added
- `upsert` support for insert method.

## [1.6.0] - 2022-10-11

### Added
- Added Support for Context Aware Authorization.
- Added Support to generate scoped skyflow bearer tokens.
## [1.5.0] - 2022-04-12

### Added
- support for application/x-www-form-urlencoded and multipart/form-data content-type's in connections.

## [1.4.1] - 2022-03-29

### Fixed 
- Request headers not getting overridden due to case sensitivity

## [1.4.0] - 2022-03-15

### Changed

- deprecated `isValid` in favour of `isExpired`

## [1.3.0] - 2022-02-24

### Added

- `requestId` in error logs and error responses for API Errors
- `isValid` method for validating Service Account bearer token

## [1.2.0] - 2022-01-11

### Added
- Logging functionality
- `Configuration.setLogLevel` function for setting the package-level LogLevel
- `generateBearerTokenFromCreds` function which takes credentials as string

### Changed
- Renamed and deprecated `GenerateToken` in favor of `generateBearerToken`
- `vaultID` and `vaultURL` are optional in `SkyflowConfiguration` constructor

## [1.1.0] - 2021-11-10
### Added
- `insert` vault API
- `detokenize` vault API
- `getById` vault API
- `invokeConnection` 
## [1.0.1] - 2021-10-20
### Added 
-  Service Account Token generation