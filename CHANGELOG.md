# Changelog

All notable changes to this project will be documented in this file.

## [2.0.4] - 2025-05-15
### Changed
- Credential JSON field names follow Java camelCase convention: `clientId`, `keyId`, `tokenUri`. Legacy all-caps forms (`clientID`, `keyID`, `tokenURI`) permanently accepted — no migration required.
- Get and Query response maps now return `skyflowId` (camelCase). Legacy `skyflow_id` key retained alongside for backward compatibility; deprecated and will be removed in an upcoming release.
- `GetRequest` and `DetokenizeRequest`: added `downloadUrl()` / `getDownloadUrl()` following acronym-as-word convention. Old `downloadURL()` / `getDownloadURL()` kept as `@Deprecated` delegates.
- `QueryResponse`: added `getErrors()` accessor.
- Removed SDK-level null/empty field value validation from Insert and Update — backend validates per API spec.
