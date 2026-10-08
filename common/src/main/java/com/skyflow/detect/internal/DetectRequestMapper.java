package com.skyflow.detect.internal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.skyflow.detect.Audio;
import com.skyflow.detect.Bleep;
import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CustomType;
import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.Document;
import com.skyflow.detect.Entity;
import com.skyflow.detect.FileMapping;
import com.skyflow.detect.Image;
import com.skyflow.detect.Media;
import com.skyflow.detect.ObjectEntity;
import com.skyflow.detect.Pdf;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ShiftDates;
import com.skyflow.detect.Transformation;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.detect.rest.resources.filesv2.requests.DeidentifyFileRequestV2;
import com.skyflow.generated.detect.rest.resources.filesv2.types.DeidentifyFileRequestV2DataFormat;
import com.skyflow.generated.detect.rest.resources.filesv2.types.DeidentifyFileRequestV2DataSource;
import com.skyflow.generated.detect.rest.resources.guardrailsv2.requests.DetectGuardrailsRequestV2;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.DeidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.ReidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.types.AudioV2;
import com.skyflow.generated.detect.rest.types.AudioV2Bleep;
import com.skyflow.generated.detect.rest.types.AudioV2OutputTranscription;
import com.skyflow.generated.detect.rest.types.DetectConfigV2;
import com.skyflow.generated.detect.rest.types.DetectCustomTypes;
import com.skyflow.generated.detect.rest.types.DetectEntity;
import com.skyflow.generated.detect.rest.types.DetectEntityDeidentificationType;
import com.skyflow.generated.detect.rest.types.DetectEntityEntityType;
import com.skyflow.generated.detect.rest.types.DetectObjectEntity;
import com.skyflow.generated.detect.rest.types.DetectObjectEntityDeidentificationType;
import com.skyflow.generated.detect.rest.types.DetectObjectEntityEntityType;
import com.skyflow.generated.detect.rest.types.DetectReturnEntities;
import com.skyflow.generated.detect.rest.types.DocumentV2;
import com.skyflow.generated.detect.rest.types.DocumentV2Pdf;
import com.skyflow.generated.detect.rest.types.DocumentV2PdfProcessingMode;
import com.skyflow.generated.detect.rest.types.EntityTransformation;
import com.skyflow.generated.detect.rest.types.ImageV2;
import com.skyflow.generated.detect.rest.types.ImageV2MaskingMethod;
import com.skyflow.generated.detect.rest.types.RedactionLevelEntityName;
import com.skyflow.generated.detect.rest.types.RedactionLevelRedactionType;
import com.skyflow.generated.detect.rest.types.TransformationShiftDates;

/**
 * Converts public SDK request models into generated V2 API requests. Internal to the SDK.
 */
public final class DetectRequestMapper {
    private DetectRequestMapper() {
    }

    public static DeidentifyStringRequestV2 toDeidentifyStringRequest(DeidentifyStringRequest request, String vaultId) {
        DeidentifyStringRequestV2._FinalStage builder = DeidentifyStringRequestV2.builder().text(request.getText());
        if (request.getConfigurationId() != null) {
            builder.configurationId(request.getConfigurationId());
        }
        if (request.getConfiguration() != null) {
            builder.configuration(toDetectConfig(request.getConfiguration(), vaultId));
        }
        return builder.build();
    }

    /**
     * Builds the submit request. For file-based requests this reads and base64-encodes the file and infers
     * the format from its extension when none was given.
     */
    public static DeidentifyFileRequestV2 toDeidentifyFileRequest(DeidentifyFileRequest request, String vaultId)
            throws SkyflowException {
        String value = request.getValue();
        FileDataFormat format = request.getDataFormat();
        File file = request.getFile();
        if (file != null) {
            value = encodeFile(file);
            if (format == null) {
                format = inferFormat(file.getName());
            }
        }
        // A local file is always sent as BASE64, whatever dataSource the builder was left with.
        DataSourceType dataSource = file != null ? DataSourceType.BASE64 : request.getDataSource();
        DeidentifyFileRequestV2._FinalStage builder = DeidentifyFileRequestV2.builder()
                .dataSource(DeidentifyFileRequestV2DataSource.valueOf(dataSource.name()))
                .value(value);
        if (format != null) {
            // The wire value for a file format is the lower-case extension.
            builder.dataFormat(DeidentifyFileRequestV2DataFormat.valueOf(format.name().toLowerCase()));
        }
        if (request.getConfigurationId() != null) {
            builder.configurationId(request.getConfigurationId());
        }
        if (request.getConfiguration() != null) {
            builder.configuration(toDetectConfig(request.getConfiguration(), vaultId));
        }
        return builder.build();
    }

    static String encodeFile(File file) throws SkyflowException {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
        } catch (IOException | OutOfMemoryError e) {
            LogUtil.printErrorLog(BaseUtils.parameterizedString(ErrorLogs.FAILED_TO_ENCODE_FILE_IN_DEIDENTIFY_FILE.getLog(), "detect"));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), BaseErrorMessage.FailedToEncodeFile.getMessage());
        }
    }

    /** @return the format matching the file extension, or null when unknown so the server infers it */
    static FileDataFormat inferFormat(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return null;
        }
        try {
            return FileDataFormat.valueOf(fileName.substring(dot + 1).toUpperCase());
        } catch (IllegalArgumentException unknown) {
            return null;
        }
    }

    public static ReidentifyStringRequestV2 toReidentifyStringRequest(ReidentifyStringRequest request, String vaultId) {
        ReidentifyStringRequestV2._FinalStage builder = ReidentifyStringRequestV2.builder()
                .vaultId(vaultId)
                .text(request.getText());
        if (request.getRedactionLevel() != null) {
            builder.redactionLevel(toRedactionLevels(request.getRedactionLevel()));
        }
        return builder.build();
    }

    public static DetectGuardrailsRequestV2 toCheckGuardrailsRequest(CheckGuardrailsRequest request, String vaultId) {
        DetectGuardrailsRequestV2._FinalStage builder = DetectGuardrailsRequestV2.builder()
                .text(request.getText())
                .vaultId(vaultId);
        if (request.getCheckToxicity() != null) {
            builder.checkToxicity(request.getCheckToxicity());
        }
        if (request.getDenyTopics() != null) {
            builder.denyTopics(request.getDenyTopics());
        }
        return builder.build();
    }

    public static DetectConfigV2 toDetectConfig(DetectConfiguration configuration, String vaultId) {
        DetectConfigV2._FinalStage builder = DetectConfigV2.builder().vaultId(vaultId);
        if (configuration.getName() != null) {
            builder.name(configuration.getName());
        }
        if (configuration.getDescription() != null) {
            builder.description(configuration.getDescription());
        }
        if (configuration.getDetect() != null) {
            builder.detect(toDetect(configuration.getDetect()));
        }
        if (configuration.getFileMapping() != null) {
            builder.fileMapping(toFileMappings(configuration.getFileMapping()));
        }
        if (configuration.getMedia() != null) {
            builder.media(toMedia(configuration.getMedia()));
        }
        return builder.build();
    }

    static com.skyflow.generated.detect.rest.types.Detect toDetect(Detect detect) {
        com.skyflow.generated.detect.rest.types.Detect.Builder builder =
                com.skyflow.generated.detect.rest.types.Detect.builder();
        if (detect.getEntities() != null) {
            List<DetectEntity> entities = new ArrayList<>();
            for (Entity entity : detect.getEntities()) {
                if (entity != null) {
                    entities.add(toEntity(entity));
                }
            }
            builder.entities(entities);
        }
        if (detect.getObjectEntities() != null) {
            List<DetectObjectEntity> objectEntities = new ArrayList<>();
            for (ObjectEntity objectEntity : detect.getObjectEntities()) {
                if (objectEntity != null) {
                    objectEntities.add(toObjectEntity(objectEntity));
                }
            }
            builder.objectEntities(objectEntities);
        }
        if (detect.getRestrict() != null) {
            builder.restrict(detect.getRestrict());
        }
        if (detect.getSkip() != null) {
            builder.skip(detect.getSkip());
        }
        if (detect.getCustomTypes() != null) {
            List<DetectCustomTypes> customTypes = new ArrayList<>();
            for (CustomType customType : detect.getCustomTypes()) {
                if (customType != null) {
                    customTypes.add(toCustomType(customType));
                }
            }
            builder.customTypes(customTypes);
        }
        if (detect.getReturnEntities() != null) {
            builder.returnEntities(DetectReturnEntities.valueOf(detect.getReturnEntities().name()));
        }
        return builder.build();
    }

    static DetectEntity toEntity(Entity entity) {
        DetectEntity.Builder builder = DetectEntity.builder();
        if (entity.getEntityType() != null) {
            builder.entityType(DetectEntityEntityType.valueOf(entity.getEntityType().name()));
        }
        if (entity.getDeidentificationType() != null) {
            builder.deidentificationType(DetectEntityDeidentificationType.valueOf(entity.getDeidentificationType().name()));
        }
        if (entity.getDestination() != null) {
            builder.destination(entity.getDestination());
        }
        if (entity.getTransformation() != null) {
            builder.transformation(toTransformation(entity.getTransformation()));
        }
        return builder.build();
    }

    static EntityTransformation toTransformation(Transformation transformation) {
        EntityTransformation.Builder builder = EntityTransformation.builder();
        ShiftDates shiftDates = transformation.getShiftDates();
        if (shiftDates != null) {
            TransformationShiftDates.Builder shift = TransformationShiftDates.builder();
            if (shiftDates.getMinDays() != null) {
                shift.minDays(shiftDates.getMinDays());
            }
            if (shiftDates.getMaxDays() != null) {
                shift.maxDays(shiftDates.getMaxDays());
            }
            builder.shiftDates(shift.build());
        }
        return builder.build();
    }

    static DetectCustomTypes toCustomType(CustomType customType) {
        DetectCustomTypes.Builder builder = DetectCustomTypes.builder();
        if (customType.getLabel() != null) {
            builder.label(customType.getLabel());
        }
        if (customType.getMatch() != null) {
            builder.match(customType.getMatch());
        }
        return builder.build();
    }

    static DetectObjectEntity toObjectEntity(ObjectEntity objectEntity) {
        DetectObjectEntity.Builder builder = DetectObjectEntity.builder();
        if (objectEntity.getEntityType() != null) {
            builder.entityType(DetectObjectEntityEntityType.valueOf(objectEntity.getEntityType().name()));
        }
        if (objectEntity.getDeidentificationType() != null) {
            builder.deidentificationType(
                    DetectObjectEntityDeidentificationType.valueOf(objectEntity.getDeidentificationType().name()));
        }
        return builder.build();
    }

    static List<com.skyflow.generated.detect.rest.types.FileMapping> toFileMappings(List<FileMapping> fileMappings) {
        List<com.skyflow.generated.detect.rest.types.FileMapping> result = new ArrayList<>();
        for (FileMapping mapping : fileMappings) {
            if (mapping == null) {
                continue;
            }
            com.skyflow.generated.detect.rest.types.FileMapping.Builder builder =
                    com.skyflow.generated.detect.rest.types.FileMapping.builder();
            if (mapping.getSource() != null) {
                builder.source(mapping.getSource());
            }
            if (mapping.getDestination() != null) {
                builder.destination(mapping.getDestination());
            }
            if (mapping.getOutputFileNameSuffix() != null) {
                builder.outputFileNameSuffix(mapping.getOutputFileNameSuffix());
            }
            if (mapping.getEntitiesDestination() != null) {
                builder.entitiesDestination(mapping.getEntitiesDestination());
            }
            if (mapping.getObjectEntitiesDestination() != null) {
                builder.objectEntitiesDestination(mapping.getObjectEntitiesDestination());
            }
            result.add(builder.build());
        }
        return result;
    }

    static com.skyflow.generated.detect.rest.types.Media toMedia(Media media) {
        com.skyflow.generated.detect.rest.types.Media.Builder builder =
                com.skyflow.generated.detect.rest.types.Media.builder();
        if (media.getAudio() != null) {
            builder.audio(toAudio(media.getAudio()));
        }
        if (media.getDocument() != null) {
            builder.document(toDocument(media.getDocument()));
        }
        if (media.getImage() != null) {
            builder.image(toImage(media.getImage()));
        }
        return builder.build();
    }

    static AudioV2 toAudio(Audio audio) {
        AudioV2.Builder builder = AudioV2.builder();
        if (audio.getOutputProcessedAudio() != null) {
            builder.outputProcessedAudio(audio.getOutputProcessedAudio());
        }
        if (audio.getOutputTranscription() != null) {
            builder.outputTranscription(AudioV2OutputTranscription.valueOf(audio.getOutputTranscription().name()));
        }
        Bleep bleep = audio.getBleep();
        if (bleep != null) {
            AudioV2Bleep.Builder bleepBuilder = AudioV2Bleep.builder();
            if (bleep.getStartPadding() != null) {
                bleepBuilder.startPadding(bleep.getStartPadding());
            }
            if (bleep.getStopPadding() != null) {
                bleepBuilder.stopPadding(bleep.getStopPadding());
            }
            if (bleep.getFrequency() != null) {
                bleepBuilder.frequency(bleep.getFrequency());
            }
            if (bleep.getGain() != null) {
                bleepBuilder.gain(bleep.getGain());
            }
            builder.bleep(bleepBuilder.build());
        }
        return builder.build();
    }

    static DocumentV2 toDocument(Document document) {
        DocumentV2.Builder builder = DocumentV2.builder();
        Pdf pdf = document.getPdf();
        if (pdf != null) {
            DocumentV2Pdf.Builder pdfBuilder = DocumentV2Pdf.builder();
            if (pdf.getProcessingMode() != null) {
                pdfBuilder.processingMode(DocumentV2PdfProcessingMode.valueOf(pdf.getProcessingMode().name()));
            }
            if (pdf.getDensity() != null) {
                pdfBuilder.density(pdf.getDensity());
            }
            if (pdf.getMaxResolution() != null) {
                pdfBuilder.maxResolution(pdf.getMaxResolution());
            }
            builder.pdf(pdfBuilder.build());
        }
        return builder.build();
    }

    static ImageV2 toImage(Image image) {
        ImageV2.Builder builder = ImageV2.builder();
        if (image.getOutputProcessedImage() != null) {
            builder.outputProcessedImage(image.getOutputProcessedImage());
        }
        if (image.getOutputOcrText() != null) {
            builder.outputOcrText(image.getOutputOcrText());
        }
        if (image.getMaskingMethod() != null) {
            builder.maskingMethod(ImageV2MaskingMethod.valueOf(image.getMaskingMethod().name()));
        }
        return builder.build();
    }

    public static List<com.skyflow.generated.detect.rest.types.RedactionLevel> toRedactionLevels(List<RedactionLevel> levels) {
        List<com.skyflow.generated.detect.rest.types.RedactionLevel> result = new ArrayList<>();
        for (RedactionLevel level : levels) {
            if (level == null) {
                continue;
            }
            com.skyflow.generated.detect.rest.types.RedactionLevel.Builder builder =
                    com.skyflow.generated.detect.rest.types.RedactionLevel.builder();
            if (level.getEntityName() != null) {
                builder.entityName(RedactionLevelEntityName.valueOf(level.getEntityName().name()));
            }
            if (level.getRedactionType() != null) {
                builder.redactionType(RedactionLevelRedactionType.valueOf(level.getRedactionType().name()));
            }
            if (level.getTokenGroupName() != null) {
                builder.tokenGroupName(level.getTokenGroupName());
            }
            if (level.getRedactionPattern() != null) {
                builder.redactionPattern(level.getRedactionPattern());
            }
            result.add(builder.build());
        }
        return result;
    }
}
