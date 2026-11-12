package com.skyflow.vault.detect.internal;

import com.skyflow.vault.detect.ReidentifyFileSource;
import com.skyflow.vault.detect.DetectRunStatus;
import com.skyflow.vault.detect.ReidentifyFileDataFormat;
import com.skyflow.vault.detect.ReidentifyFileOutput;
import com.skyflow.vault.detect.ReidentifyFileRequest;
import com.skyflow.vault.detect.ReidentifyFileResponse;
import com.skyflow.generated.common.rest.resources.filesv2.requests.ReidentifyFileRequestV2;
import com.skyflow.generated.common.rest.resources.filesv2.types.ReidentifyFileRequestV2DataFormat;
import com.skyflow.generated.common.rest.resources.filesv2.types.ReidentifyFileRequestV2DataSource;
import com.skyflow.generated.common.rest.types.ReidentifyFileResponseV2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Converts the flowvault {@code reidentifyFile} models to and from the generated V2 API types.
 * Internal to the SDK.
 */
public final class ReidentifyFileMapper {
    private ReidentifyFileMapper() {
    }

    public static ReidentifyFileRequestV2 toApiRequest(ReidentifyFileRequest request, String vaultId) {
        ReidentifyFileRequestV2._FinalStage builder = ReidentifyFileRequestV2.builder()
                .dataSource(ReidentifyFileRequestV2DataSource.valueOf(request.getDataSource().name()))
                .value(request.getValue())
                .vaultId(vaultId);
        if (request.getDataFormat() != null) {
            // The wire value for a file format is the lower-case extension.
            builder.dataFormat(ReidentifyFileRequestV2DataFormat.valueOf(request.getDataFormat().name().toLowerCase()));
        }
        if (request.getRedactionLevel() != null) {
            builder.redactionLevel(DetectRequestMapper.toRedactionLevels(request.getRedactionLevel()));
        }
        return builder.build();
    }

    /**
     * @param sourceResolver maps the wire outputType onto this SDK's {@code ReidentifySourceType}; returns null for an
     *                       unknown value
     */
    public static ReidentifyFileResponse toResponse(ReidentifyFileResponseV2 response,
                                                    Function<String, ReidentifyFileSource> sourceResolver) {
        List<ReidentifyFileOutput> outputs = new ArrayList<>();
        List<com.skyflow.generated.common.rest.types.FileOutput> apiOutputs = response.getOutput().orElse(null);
        if (apiOutputs != null) {
            for (com.skyflow.generated.common.rest.types.FileOutput apiOutput : apiOutputs) {
                if (apiOutput != null) {
                    outputs.add(toReidentifyFileOutput(apiOutput));
                }
            }
        }
        return new ReidentifyFileResponse(
                response.getStatus().map(s -> enumOrDefault(DetectRunStatus.class, s.toString(), DetectRunStatus.UNKNOWN))
                        .orElse(DetectRunStatus.UNKNOWN),
                response.getOutputType().map(t -> sourceResolver.apply(t.toString())).orElse(null),
                Collections.unmodifiableList(outputs),
                response.getMetrics().map(DetectResponseMapper::toMetrics).orElse(null));
    }

    /** The re-identify counterpart of {@link DetectResponseMapper#toFileOutput}: the format is a {@link ReidentifyFileDataFormat}. */
    static ReidentifyFileOutput toReidentifyFileOutput(com.skyflow.generated.common.rest.types.FileOutput apiOutput) {
        return new ReidentifyFileOutput(
                apiOutput.getProcessedFile().orElse(null),
                apiOutput.getProcessedFileType().map(Object::toString).orElse(null),
                apiOutput.getProcessedFileExtension()
                        .map(e -> enumOrDefault(ReidentifyFileDataFormat.class, e.toString().toUpperCase(), null))
                        .orElse(null));
    }

    /** Maps a wire value onto a public enum, tolerating values this SDK version does not know. */
    private static <E extends Enum<E>> E enumOrDefault(Class<E> type, String value, E fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException unknown) {
            return fallback;
        }
    }
}
