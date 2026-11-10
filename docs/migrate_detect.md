# Migrate to the new Detect methods (skyflow-java 2.2)

This guide is for skyvault users of the Java SDK (`com.skyflow:skyflow-java`) who call the Detect methods
`deidentifyText`, `reidentifyText`, `deidentifyFile` or `getDetectRun` from `com.skyflow.vault.detect`. Those
methods, their request and response classes, and the enums `DetectEntities`, `TokenType`,
`DetectOutputTranscriptions` and `MaskingMethod` in `com.skyflow.enums` are removed in **skyflow-java 2.2.0** and
replaced by the Detect methods in `com.skyflow.detect`, documented in the [skyvault README](../skyvault/README.md#detect).
Projects that call the removed methods stop compiling on upgrade, so pin the version explicitly and follow this guide
rather than relying on a version range.

```xml
<dependency>
    <groupId>com.skyflow</groupId>
    <artifactId>skyflow-java</artifactId>
    <version>2.2.0</version>
</dependency>
```

Client creation and the `skyflowClient.detect()` / `skyflowClient.detect(vaultId)` accessors do not change.

> [!NOTE]
> Using a FlowVault vault? FlowVault Detect is new in `skyflow-flowvault-java` and has nothing to migrate: see
> [Detect in the flowvault README](../flowvault/README.md#detect).

> [!WARNING]
> **`deidentifyFile` keeps its name but no longer waits for the result by default.** It submits the file and
> returns only the run id; `getStatus()` is `null` and nothing is written to disk until you pass `pollOptions`.
> A quick port compiles and runs, and the missing outputs only show up downstream. See
> [De-identify a file](#de-identify-a-file).

## Table of contents

- [What changed](#what-changed)
- [Method mapping](#method-mapping)
- [Step 1: move the detection policy into a configuration](#step-1-move-the-detection-policy-into-a-configuration)
- [Step 2: deidentifyText to deidentifyString](#step-2-deidentifytext-to-deidentifystring)
- [Step 3: reidentifyText to reidentifyString](#step-3-reidentifytext-to-reidentifystring)
- [Step 4: deidentifyFile, new request and response](#step-4-deidentifyfile-new-request-and-response)
- [Step 5: getDetectRun to getRun](#step-5-getdetectrun-to-getrun)
- [New methods and options](#new-methods-and-options)
- [Migration checklist](#migration-checklist)

## What changed

|  | Removed | Replacement |
|---|---|---|
| Package | `com.skyflow.vault.detect`, enums in `com.skyflow.enums` | `com.skyflow.detect` for requests, responses and Detect enums; `RedactionType` stays in `com.skyflow.enums` |
| API | `/v1/detect/...`, one file endpoint per file type | `/v2/detect/...`, one file endpoint |
| Detection policy | flat fields on every request | a stored configuration id, or one inline `DetectConfiguration` |
| Token type | `TokenFormat` with per-type lists | `deidentificationType` on each `Entity`; `VAULT_TOKEN` entities name their `table.column` |
| File polling | always, `waitTime` 1 to 64 s | opt in with `PollOptions`: `waitTime` 1 to 300 s and `maxAttempts` 1 to 23 |
| Responses | flat counts, first output only | `Metrics`, a `List<FileOutput>`, `EntityLocation`, `DetectRunStatus` |
| New capabilities |  | re-identify a file, guardrails, custom types, object entities (faces, logos), file mapping, `SKYFLOW_ID` and presigned-URL inputs |

## Method mapping

| Removed | Replacement | Request | Response |
|---|---|---|---|
| `deidentifyText(DeidentifyTextRequest)` | `deidentifyString(DeidentifyStringRequest)` | `DeidentifyStringRequest` | `DeidentifyStringResponse` |
| `reidentifyText(ReidentifyTextRequest)` | `reidentifyString(ReidentifyStringRequest)` | `ReidentifyStringRequest` | `ReidentifyStringResponse` |
| `deidentifyFile(DeidentifyFileRequest)` | `deidentifyFile(DeidentifyFileRequest)`, same name, new types | `DeidentifyFileRequest` in `com.skyflow.detect` | `DeidentifyFileResponse` in `com.skyflow.detect` |
| `getDetectRun(GetDetectRunRequest)` | `getRun(GetRunRequest)` | `GetRunRequest` | `GetRunResponse` |
| — | `reidentifyFile(ReidentifyFileRequest)` | `ReidentifyFileRequest` | `ReidentifyFileResponse` |
| — | `checkGuardrails(CheckGuardrailsRequest)` | `CheckGuardrailsRequest` | `CheckGuardrailsResponse` |

The file request and response keep their simple class names and only change package. A project that still imports
`com.skyflow.vault.detect.*` fails to compile; replace that import with `com.skyflow.detect.*`. Every method is still
reached through `skyflowClient.detect()`, takes a builder-built request, returns a response object and throws
`SkyflowException`.

## Step 1: move the detection policy into a configuration

Previously every call carried the policy as flat fields: `entities`, `tokenFormat`, `allowRegexList`,
`restrictRegexList`, `transformations` and the media options. Now the policy is a `DetectConfiguration`, supplied in
one of two ways:

- `configurationId(String)`: a configuration created once in Studio or through the management API, then referenced by
  id. This is the recommended path.
- `configuration(DetectConfiguration)`: an inline configuration built in code, for callers that need per-call control.

Both are optional and sent as given. With neither, the SDK sends a configuration holding only the vault id and the run
uses the vault's default Detect settings. Setting both is rejected by the API with HTTP 400. The SDK does not create,
update or delete stored configurations.

### Request field mapping

| Removed field (`DeidentifyTextRequest` / `DeidentifyFileRequest`) | Replacement |
|---|---|
| `entities(List<DetectEntities>)` | `Detect.entities(List<Entity>)`, one `Entity` per type with `entityType` |
| `tokenFormat.defaultType(TokenType)` | An `Entity` with `entityType(EntityType.ALL)` and the chosen `deidentificationType`; it applies to every type without its own rule |
| `tokenFormat.vaultToken(List)` | An `Entity` per type with `deidentificationType(VAULT_TOKEN)` and `destination("<TABLE>.<COLUMN>")` |
| `tokenFormat.entityUniqueCounter(List)` | An `Entity` per type with `deidentificationType(ENTITY_UNIQUE_COUNTER)` |
| `tokenFormat.entityOnly(List)` | An `Entity` per type with `deidentificationType(ENTITY_ONLY)`; `MASK` is new |
| `allowRegexList` | `Detect.skip` |
| `restrictRegexList` | `Detect.restrict` |
| `transformations(new Transformations(new DateTransformation(max, min, entities)))` | `transformation(Transformation.builder().shiftDates(ShiftDates.builder().minDays(min).maxDays(max).build()).build())` on each listed `Entity`; the entity list is gone because the rule sits on the entity |
| `pixelDensity`, `maxResolution` | `Media.document(Document.builder().pdf(Pdf.builder().density(...).maxResolution(...)))` |
| `outputProcessedImage`, `outputOcrText`, `maskingMethod` | `Media.image(Image.builder()...)` with the same three names |
| `outputProcessedAudio`, `outputTranscription(DetectOutputTranscriptions)` | `Media.audio(Audio.builder()...)` with the same two names; the two cannot be combined, so set `outputProcessedAudio(false)` when asking for a transcription |
| `bleep(AudioBleep)` with `Double` fields | `Audio.bleep(Bleep)` with `Float` paddings and `Integer` frequency and gain |
| `file(FileInput.builder().file(f).build())` / `.filePath(path)` | `file(f)` or `filePath(path)` directly on the request |
| `waitTime(Integer)` | `pollOptions(PollOptions.builder().waitTime(n).maxAttempts(m).build())` |
| `outputDirectory` | `outputDirectory`, allowed only together with `pollOptions` |
| — | New: `customTypes`, `objectEntities`, `returnEntities`, `fileMapping`, per-entity `includePrefix` / `prefixOverride`, and `dataSource` + `value` inputs |

### Enum renames

| Removed (`com.skyflow.enums`) | Replacement (`com.skyflow.detect`) |
|---|---|
| `DetectEntities` (`ALL`, `NAME`, `EMAIL_ADDRESS`, `SSN`, ...) | `EntityType`, the same values plus `NONE` |
| `TokenType` (`VAULT_TOKEN`, `ENTITY_UNIQUE_COUNTER`, `ENTITY_ONLY`) | `DeidentificationType`, same names plus `MASK` and `UNKNOWN` |
| `DetectOutputTranscriptions` | `OutputTranscriptionType`, same names plus `NONE` |
| `MaskingMethod` (`BLUR`, `BLACKBOX`) | `MaskingMethod`, same names plus `NONE` |
| `RedactionType` | Unchanged, still `com.skyflow.enums.RedactionType` and still shared with `get` and `detokenize`. The Detect API does not accept `DEFAULT` in a `RedactionLevel`; use `PLAIN_TEXT`, `MASKED` or `REDACTED`. |

### Example: the same policy before and after

**Removed**

```java
import com.skyflow.vault.detect.*;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.TokenType;

DeidentifyTextRequest request = DeidentifyTextRequest.builder()
        .text("My name is John Doe, and my email is johndoe@acme.com.")
        .entities(Arrays.asList(DetectEntities.NAME, DetectEntities.EMAIL_ADDRESS, DetectEntities.DOB))
        .tokenFormat(TokenFormat.builder()
                .defaultType(TokenType.ENTITY_UNIQUE_COUNTER)
                .vaultToken(Arrays.asList(DetectEntities.NAME, DetectEntities.EMAIL_ADDRESS))
                .build())
        .allowRegexList(Collections.singletonList("Skyflow"))
        .restrictRegexList(Collections.singletonList("[0-9]{3}-[0-9]{2}-[0-9]{4}"))
        .transformations(new Transformations(
                new DateTransformation(30, 10, Collections.singletonList(DetectEntities.DOB))))
        .build();
```

**Replacement, inline configuration**

```java
import com.skyflow.detect.*;

DetectConfiguration configuration = DetectConfiguration.builder()
        .detect(Detect.builder()
                .entities(Arrays.asList(
                        Entity.builder().entityType(EntityType.NAME)
                                .deidentificationType(DeidentificationType.VAULT_TOKEN)
                                .destination("persons.name").build(),
                        Entity.builder().entityType(EntityType.EMAIL_ADDRESS)
                                .deidentificationType(DeidentificationType.VAULT_TOKEN)
                                .destination("persons.email").build(),
                        Entity.builder().entityType(EntityType.DOB)
                                .deidentificationType(DeidentificationType.ENTITY_UNIQUE_COUNTER)
                                .transformation(Transformation.builder()
                                        .shiftDates(ShiftDates.builder().minDays(10).maxDays(30).build())
                                        .build()).build(),
                        Entity.builder().entityType(EntityType.ALL)
                                .deidentificationType(DeidentificationType.ENTITY_UNIQUE_COUNTER).build()))
                .skip(Collections.singletonList("Skyflow"))
                .restrict(Collections.singletonList("[0-9]{3}-[0-9]{2}-[0-9]{4}"))
                .build())
        .build();

DeidentifyStringRequest request = DeidentifyStringRequest.builder()
        .text("My name is John Doe, and my email is johndoe@acme.com.")
        .configuration(configuration)
        .build();
```

**Replacement, stored configuration**

```java
DeidentifyStringRequest request = DeidentifyStringRequest.builder()
        .text("My name is John Doe, and my email is johndoe@acme.com.")
        .configurationId("<CONFIGURATION_ID>")
        .build();
```

Two points differ in meaning, not only in name:

- The old request listed `entities` once and then assigned token types per list. Now the `deidentificationType` is
  stated on each entity, and the old `defaultType` becomes an `EntityType.ALL` entry placed last.
- The old `vaultToken` list only named the entity types. Now each `VAULT_TOKEN` entity also needs a
  `destination` of the form `table.column`, the column with tokenization enabled that the value is stored in.

## Step 2: deidentifyText to deidentifyString

```java
// Removed
DeidentifyTextResponse before = skyflowClient.detect().deidentifyText(request);
before.getProcessedText();
for (EntityInfo e : before.getEntities()) {
    e.getEntity(); e.getToken(); e.getValue();
    e.getTextIndex().getStart(); e.getTextIndex().getEnd();
    e.getProcessedIndex().getStart(); e.getProcessedIndex().getEnd();
    e.getScores();
}
before.getWordCount(); before.getCharCount();

// Replacement
DeidentifyStringResponse after = skyflowClient.detect().deidentifyString(request);
after.getProcessedText();
for (DetectedEntity e : after.getEntities()) {
    e.getEntityType(); e.getToken(); e.getValue();
    e.getLocation().getStartIndex(); e.getLocation().getEndIndex();
    e.getLocation().getStartIndexProcessed(); e.getLocation().getEndIndexProcessed();
    e.getEntityScores();
}
Metrics metrics = after.getMetrics();              // null when the server sends none
metrics.getWordCount(); metrics.getCharacterCount();
```

| Removed (`DeidentifyTextResponse`) | Replacement (`DeidentifyStringResponse`) |
|---|---|
| `getEntities()` of `EntityInfo` | `getEntities()` of `DetectedEntity`, never `null` |
| `EntityInfo.getEntity()` | `DetectedEntity.getEntityType()` (a `String`, so it can carry custom-type labels) |
| `getTextIndex().getStart()` / `getEnd()` | `getLocation().getStartIndex()` / `getEndIndex()` |
| `getProcessedIndex().getStart()` / `getEnd()` | `getLocation().getStartIndexProcessed()` / `getEndIndexProcessed()` |
| `getScores()` | `getEntityScores()` |
| `getWordCount()`, `getCharCount()` as `int` | `getMetrics().getWordCount()`, `getMetrics().getCharacterCount()` as `Integer`, may be `null` |

`getValue()` is populated only when the configuration sets `returnEntities` to `ALL`. The default
`EXCLUDE_SENSITIVE_DATA` returns an empty value.

## Step 3: reidentifyText to reidentifyString

Only vault tokens can be re-identified. Placeholders from `ENTITY_UNIQUE_COUNTER`, `ENTITY_ONLY` or `MASK` are not
stored, so they come back unchanged. The text to re-identify is the `processedText` of a `VAULT_TOKEN`
de-identification, in which a token looks like `[NAME_daa78453-9ca9-4ba5-a3ef-91934c11196c]`.

The old request took three lists of entity types. The new one takes a list of `RedactionLevel` rules, each naming an
entity type and a redaction type.

```java
// Removed
ReidentifyTextRequest request = ReidentifyTextRequest.builder()
        .text("<TEXT_WITH_VAULT_TOKENS>")
        .plainTextEntities(Collections.singletonList(DetectEntities.NAME))
        .maskedEntities(Collections.singletonList(DetectEntities.EMAIL_ADDRESS))
        .redactedEntities(Collections.singletonList(DetectEntities.SSN))
        .build();
ReidentifyTextResponse before = skyflowClient.detect().reidentifyText(request);

// Replacement
ReidentifyStringRequest request = ReidentifyStringRequest.builder()
        .text("<TEXT_WITH_VAULT_TOKENS>")   // e.g. getProcessedText() of a deidentifyString call that used VAULT_TOKEN
        .redactionLevel(Arrays.asList(
                RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.PLAIN_TEXT).build(),
                RedactionLevel.builder().entityName(EntityType.EMAIL_ADDRESS).redactionType(RedactionType.MASKED).build(),
                RedactionLevel.builder().entityName(EntityType.SSN).redactionType(RedactionType.REDACTED).build()))
        .build();
ReidentifyStringResponse after = skyflowClient.detect().reidentifyString(request);
```

| Removed list | Replacement rule |
|---|---|
| `plainTextEntities` | `RedactionLevel.builder().entityName(x).redactionType(RedactionType.PLAIN_TEXT)` |
| `maskedEntities` | `RedactionLevel.builder().entityName(x).redactionType(RedactionType.MASKED)` |
| `redactedEntities` | `RedactionLevel.builder().entityName(x).redactionType(RedactionType.REDACTED)` |

`getProcessedText()` is unchanged and `getMetrics()` is new. Without rules every token gets its original value. The
skyvault `RedactionLevel` has only `entityName` and `redactionType`; `tokenGroupName` and `redactionPattern` belong
to the flowvault SDK.

## Step 4: deidentifyFile, new request and response

The method keeps its name. The request and response classes keep their simple names but move to `com.skyflow.detect`
with new shapes. Three things change at once: the file input, the policy, and the polling.

```java
// Removed (com.skyflow.vault.detect): always polled, result in one response, processed copy written to the temp directory
DeidentifyFileResponse before = skyflowClient.detect().deidentifyFile(DeidentifyFileRequest.builder()
        .file(FileInput.builder().filePath("/data/intake/patient-notes.pdf").build())
        .entities(Collections.singletonList(DetectEntities.ALL))
        .tokenFormat(TokenFormat.builder().defaultType(TokenType.ENTITY_UNIQUE_COUNTER).build())
        .pixelDensity(200)
        .maxResolution(3000)
        .outputDirectory("/data/processed")
        .waitTime(64)
        .build());
before.getStatus();            // String
before.getFileBase64();        // first output only
before.getEntities();          // List<FileEntityInfo>
before.getPageCount();

// Replacement (com.skyflow.detect): same behaviour, opt in to polling
DeidentifyFileResponse after = skyflowClient.detect().deidentifyFile(DeidentifyFileRequest.builder()
        .filePath("/data/intake/patient-notes.pdf")
        .configurationId("<CONFIGURATION_ID>")      // or .configuration(...) with media.document.pdf density 200, maxResolution 3000
        .pollOptions(PollOptions.builder().waitTime(64).build())
        .outputDirectory("/data/processed")
        .build());
after.getStatus();             // DetectRunStatus
for (FileOutput output : after.getOutput()) {       // every output, not only the first
    output.getProcessedFile(); output.getProcessedFileType(); output.getProcessedFileExtension();
}
after.getMetrics().getPages();

switch (after.getStatus()) {
    case SUCCESS:                                   // outputs are on disk under /data/processed
        break;
    case IN_PROGRESS: case QUEUED:                  // the wait ran out: resume with getRun(after.getRunId())
        break;
    default:                                        // FAILED or UNKNOWN: after.getMessage()
}
```

Or submit only and retrieve later, which the old method could not do:

```java
DeidentifyFileResponse submitted = skyflowClient.detect().deidentifyFile(DeidentifyFileRequest.builder()
        .filePath("/data/intake/patient-notes.pdf")
        .configurationId("<CONFIGURATION_ID>")
        .build());
String runId = submitted.getRunId();           // getStatus() is null, nothing written to disk
```

### File response mapping

| Removed (`DeidentifyFileResponse` in `com.skyflow.vault.detect`) | Replacement (`DeidentifyFileResponse` in `com.skyflow.detect`, and `GetRunResponse`) |
|---|---|
| `getRunId()` | `getRunId()` |
| `getStatus()` as `String` (`IN_PROGRESS`, `FAILED`, `SUCCESS`) | `getStatus()` as `DetectRunStatus` (`SUCCESS`, `FAILED`, `IN_PROGRESS`, `QUEUED`, `UNKNOWN`), `null` when not polled |
| `getFileBase64()`, `getType()`, `getExtension()` for the first output | `getOutput()`, a `List<FileOutput>` with `getProcessedFile()`, `getProcessedFileType()`, `getProcessedFileExtension()`; `getOutputType()` says whether `processedFile` is base64 or a presigned URL |
| `getFile()` as a `FileInfo` for a temp copy | Not provided; outputs are written to `outputDirectory` instead |
| `getEntities()` as `List<FileEntityInfo>` | The `FileOutput` whose type is `ENTITIES` (a JSON file) |
| `getWordCount()`, `getCharCount()` | `getMetrics().getWordCount()`, `getCharacterCount()` |
| `getSizeInKb()`, `getDurationInSeconds()` as `Float` | `getMetrics().getSize()`, `getDuration()` as `Double` |
| `getPageCount()`, `getSlideCount()` | `getMetrics().getPages()`, `getSlides()`; at most one of pages, slides and duration is set |
| — | `getOutputType()`, `getMessage()` (the detail of a failed run) |

### Behaviour differences to check

- **Polling is opt in.** Without `pollOptions` the call returns the run id immediately. Callers that relied on the result
  being complete must add `pollOptions`.
- **Budget limits changed.** `waitTime` defaults to 60 and tops out at 300, where the old method defaulted to 64 and
  capped at 64. `maxAttempts` is new, default and maximum 23.
- **Elapsed time is measured by the clock.** The old method counted only its planned sleeps, so the time spent on the
  status calls themselves was not charged to the budget. The new one measures real elapsed time and never exceeds
  `waitTime`.
- **No temp file.** The old method wrote a copy of the processed file to the Java temp directory and exposed it through
  `getFile()`. The new one writes only to `outputDirectory`, or the working directory when none is set, and only for
  local file inputs that reached `SUCCESS` with base64 outputs. Remove any code that cleaned up temp copies.
- **Output file names.** The redacted file is `processed-<input name>.<extension>`. The entities file is
  `processed-entities-<input name>.json`, and object entities `processed-object-entities-<input name>.json` for PDFs,
  Office documents and images.
- **All outputs are returned.** The old response surfaced only the first output. `getOutput()` returns the full list.
- **`UNKNOWN` ends polling.** The old method kept calling the run endpoint on any status it did not recognize. The new one
  stops and returns `UNKNOWN`; check for it and call `getRun` later.
- **Errors are not rewrapped.** The old method replaced any polling failure with a generic "polling for results failed"
  message. The new one throws the `SkyflowException` of the failed lookup with its HTTP code and request id intact.
- **Validation is stricter.** `waitTime` and `maxAttempts` out of range, `outputDirectory` without `pollOptions`, and
  supplying more than one of `file`, `filePath` and `value` fail before any network call.
- **Supported inputs widen.** `dataSource` can be `SKYFLOW_ID` for a file already in the vault, or `PRESIGNED_URL`, in
  addition to a local file.

## Step 5: getDetectRun to getRun

```java
// Removed
DeidentifyFileResponse run = skyflowClient.detect().getDetectRun(
        GetDetectRunRequest.builder().runId(runId).build());

// Replacement
GetRunResponse run = skyflowClient.detect().getRun(GetRunRequest.builder().runId(runId).build());
if (run.getStatus() == DetectRunStatus.SUCCESS) {
    for (FileOutput output : run.getOutput()) {
        if (run.getOutputType() == DeidentifySourceType.BASE64) {
            byte[] bytes = Base64.getDecoder().decode(output.getProcessedFile());
            // write bytes as processed-<name>.<output.getProcessedFileExtension()>
        }   // PRESIGNED_URL: download it yourself; do not log the URL
    }
} else if (run.getStatus() == DetectRunStatus.FAILED) {
    System.err.println("Run failed: " + run.getMessage());
}
```

The old call returned the file response type and also wrote the processed file to the temp directory. `getRun` returns
`GetRunResponse`, which has the same fields as the new `DeidentifyFileResponse` with `getRunId()` echoed from the
request, and writes nothing. A blank run id is rejected before the call.

## New methods and options

- **`reidentifyFile(ReidentifyFileRequest)`**: synchronous re-identification of a file sent as base64 or a presigned
  URL, with the same `RedactionLevel` rules as strings. The output comes back in the response and the SDK writes nothing
  to disk. See [Reidentify file](../skyvault/README.md#reidentify-file).
- **`checkGuardrails(CheckGuardrailsRequest)`**: toxicity and denied-topic screening. Toxicity is evaluated only when
  `checkToxicity(true)` is set; with `denyTopics` alone the response reports `toxic=false` even for toxic text, and a
  request with neither is rejected with HTTP 400. See [Check guardrails](../skyvault/README.md#check-guardrails).
- **`customTypes`** on `Detect`: regex-defined entities with a label of your choosing, de-identified like any other
  entity.
- **`objectEntities`** on `Detect`: `FACE`, `LICENSE_PLATE`, `LOGO` and `SIGNATURE` detection in images.
- **`returnEntities`** on `Detect`: control whether raw values come back.
- **`fileMapping`** on `DetectConfiguration`: per-column handling for structured files.
- **`processingMode`** on `Pdf`: `OCR` or `TEXT_LAYER`.
- **`includePrefix` / `prefixOverride`** on `Entity` and `CustomType`: control the prefix of `VAULT_TOKEN` tokens.
  `includePrefix(NO)` returns raw tokens that cannot be re-identified.

## Migration checklist

1. Upgrade to `com.skyflow:skyflow-java:2.2.0`.
2. Create a stored Detect configuration in Studio that reproduces your policy, or build it inline with
   `DetectConfiguration` using the field mapping above.
3. Replace imports from `com.skyflow.vault.detect` and the removed enums with `com.skyflow.detect`.
4. Rename the calls: `deidentifyText` to `deidentifyString`, `reidentifyText` to `reidentifyString`, `getDetectRun` to
   `getRun`. `deidentifyFile` keeps its name; switch its request and response to the `com.skyflow.detect` classes.
5. Give every `VAULT_TOKEN` entity a `destination("<TABLE>.<COLUMN>")`.
6. Add `pollOptions` to every file call that expects a completed result.
7. Replace reads of `getFile()`, `getFileBase64()` and `getEntities()` on file responses with a loop over
   `getOutput()` that branches on `getOutputType()`, and reads of flat counts with `getMetrics()`.
8. Switch reidentify calls from the three entity lists to `RedactionLevel` rules, and do not use `RedactionType.DEFAULT`
   there.
9. Handle `DetectRunStatus.UNKNOWN` and a `null` status on file responses.
10. Remove any code that cleaned up the temp directory copies.
11. Print `getHttpCode()`, `getMessage()` and `getRequestId()` from `SkyflowException` in your error handling.
12. Run your integration tests against the detect vault and compare outputs with the previous version for the same inputs.
