# Changelog
All notable changes to this project will be documented in this file.

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
- Request headers not getting overriden due to case sensitivity

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