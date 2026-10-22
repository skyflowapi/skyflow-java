package com.skyflow.detect.internal;

import com.skyflow.detect.CheckGuardrailsResponse;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyFileResponse;
import com.skyflow.detect.DeidentifyStringResponse;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.GetRunResponse;
import com.skyflow.detect.GuardrailsValidation;
import com.skyflow.detect.DetectedEntity;
import com.skyflow.detect.EntityLocation;
import com.skyflow.detect.Metrics;
import com.skyflow.detect.ReidentifyStringResponse;
import com.skyflow.generated.detect.rest.types.DeidentifyStringResponseV2;
import com.skyflow.generated.detect.rest.types.DeidentifyStringResponseV2DetectedEntity;
import com.skyflow.generated.detect.rest.types.DeidentifyStringResponseV2EntityLocation;
import com.skyflow.generated.detect.rest.types.DetectGuardrailsResponseV2;
import com.skyflow.generated.detect.rest.types.DetectRunsResponseV2;
import com.skyflow.generated.detect.rest.types.ReidentifyStringResponseV2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Converts generated V2 API responses into public SDK response models. Internal to the SDK.
 */
public final class DetectResponseMapper {
    private DetectResponseMapper() {
    }

    public static DeidentifyStringResponse toDeidentifyStringResponse(DeidentifyStringResponseV2 response) {
        List<DetectedEntity> entities = new ArrayList<>();
        List<DeidentifyStringResponseV2DetectedEntity> apiEntities = response.getEntities().orElse(null);
        if (apiEntities != null) {
            for (DeidentifyStringResponseV2DetectedEntity apiEntity : apiEntities) {
                if (apiEntity != null) {
                    entities.add(toDetectedEntity(apiEntity));
                }
            }
        }
        return new DeidentifyStringResponse(
                response.getProcessedText().orElse(null),
                Collections.unmodifiableList(entities),
                response.getMetrics().map(DetectResponseMapper::toMetrics).orElse(null));
    }

    public static ReidentifyStringResponse toReidentifyStringResponse(ReidentifyStringResponseV2 response) {
        return new ReidentifyStringResponse(
                response.getProcessedText().orElse(null),
                response.getMetrics().map(DetectResponseMapper::toMetrics).orElse(null));
    }

    static DetectedEntity toDetectedEntity(DeidentifyStringResponseV2DetectedEntity apiEntity) {
        Map<String, Double> scores = apiEntity.getEntityScores().orElse(null);
        return new DetectedEntity(
                apiEntity.getToken().orElse(null),
                apiEntity.getValue().orElse(null),
                apiEntity.getLocation().map(DetectResponseMapper::toEntityLocation).orElse(null),
                apiEntity.getEntityType().orElse(null),
                scores == null ? Collections.<String, Double>emptyMap() : Collections.unmodifiableMap(scores));
    }

    static EntityLocation toEntityLocation(DeidentifyStringResponseV2EntityLocation location) {
        return new EntityLocation(
                location.getStartIndex().orElse(null),
                location.getEndIndex().orElse(null),
                location.getStartIndexProcessed().orElse(null),
                location.getEndIndexProcessed().orElse(null));
    }

    public static CheckGuardrailsResponse toCheckGuardrailsResponse(DetectGuardrailsResponseV2 response) {
        return new CheckGuardrailsResponse(
                response.getText(),
                enumOrDefault(GuardrailsValidation.class,
                        response.getValidation() == null ? null : response.getValidation().toString(), GuardrailsValidation.UNKNOWN),
                response.getToxic().orElse(null),
                response.getDeniedTopic().orElse(null));
    }

    public static GetRunResponse toGetRunResponse(String runId, DetectRunsResponseV2 response) {
        List<FileOutput> outputs = new ArrayList<>();
        List<com.skyflow.generated.detect.rest.types.FileOutput> apiOutputs = response.getOutput().orElse(null);
        if (apiOutputs != null) {
            for (com.skyflow.generated.detect.rest.types.FileOutput apiOutput : apiOutputs) {
                if (apiOutput != null) {
                    outputs.add(toFileOutput(apiOutput));
                }
            }
        }
        return new GetRunResponse(
                runId,
                response.getStatus().map(s -> enumOrDefault(DetectRunStatus.class, s.toString(), DetectRunStatus.UNKNOWN))
                        .orElse(DetectRunStatus.UNKNOWN),
                response.getOutputType().map(t -> enumOrDefault(DataSourceType.class, t.toString(), null)).orElse(null),
                Collections.unmodifiableList(outputs),
                response.getMetrics().map(DetectResponseMapper::toMetrics).orElse(null),
                response.getMessage().orElse(null));
    }

    /** Response for a submit-only call: run id and nothing else. */
    public static DeidentifyFileResponse submittedDeidentifyFileResponse(String runId) {
        return new DeidentifyFileResponse(runId, null, null, Collections.<FileOutput>emptyList(), null, null);
    }

    /** Merges the submit's run id with the polled run. */
    public static DeidentifyFileResponse toDeidentifyFileResponse(String runId, GetRunResponse run) {
        return new DeidentifyFileResponse(runId, run.getStatus(), run.getOutputType(), run.getOutput(),
                run.getMetrics(), run.getMessage());
    }

    public static FileOutput toFileOutput(com.skyflow.generated.detect.rest.types.FileOutput apiOutput) {
        return new FileOutput(
                apiOutput.getProcessedFile().orElse(null),
                apiOutput.getProcessedFileType().map(Object::toString).orElse(null),
                apiOutput.getProcessedFileExtension()
                        .map(e -> enumOrDefault(FileDataFormat.class, e.toString().toUpperCase(), null))
                        .orElse(null));
    }

    /** Maps a wire value onto a public enum, tolerating values this SDK version does not know. */
    static <E extends Enum<E>> E enumOrDefault(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }

    public static Metrics toMetrics(com.skyflow.generated.detect.rest.types.Metrics metrics) {
        return new Metrics(
                metrics.getSize().orElse(null),
                metrics.getWordCount().orElse(null),
                metrics.getCharacterCount().orElse(null),
                metrics.getSlides().orElse(null),
                metrics.getPages().orElse(null),
                metrics.getDuration().orElse(null));
    }
}
