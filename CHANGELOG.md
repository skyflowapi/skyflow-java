# Changelog
All notable changes to this project will be documented in this file.


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