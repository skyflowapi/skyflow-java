package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DetectRunStatus;
import com.skyflow.detect.FileOutput;
import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.detect.ReidentifyFileResponse;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.ReidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.resources.filesv2.types.ReidentifyFileRequestV2DataFormat;
import com.skyflow.generated.detect.rest.resources.filesv2.types.ReidentifyFileRequestV2DataSource;
import com.skyflow.generated.detect.rest.types.ReidentifyFileResponseV2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    public static ReidentifyFileResponse toResponse(ReidentifyFileResponseV2 response) {
        List<FileOutput> outputs = new ArrayList<>();
        List<com.skyflow.generated.detect.rest.types.FileOutput> apiOutputs = response.getOutput().orElse(null);
        if (apiOutputs != null) {
            for (com.skyflow.generated.detect.rest.types.FileOutput apiOutput : apiOutputs) {
                if (apiOutput != null) {
                    outputs.add(DetectResponseMapper.toFileOutput(apiOutput));
                }
            }
        }
        return new ReidentifyFileResponse(
                response.getStatus().map(s -> enumOrDefault(DetectRunStatus.class, s.toString(), DetectRunStatus.UNKNOWN))
                        .orElse(DetectRunStatus.UNKNOWN),
                response.getOutputType().map(t -> enumOrDefault(DataSourceType.class, t.toString(), null)).orElse(null),
                Collections.unmodifiableList(outputs),
                response.getMetrics().map(DetectResponseMapper::toMetrics).orElse(null));
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
