package com.skyflow.detect.internal;

import com.skyflow.detect.Audio;
import com.skyflow.detect.Bleep;
import com.skyflow.detect.CustomType;
import com.skyflow.detect.DeidentificationType;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.Document;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.DeidentifyFileDataFormat;
import com.skyflow.detect.FileMapping;
import com.skyflow.detect.Image;
import com.skyflow.detect.IncludePrefix;
import com.skyflow.detect.MaskingMethod;
import com.skyflow.detect.Media;
import com.skyflow.detect.ObjectDeidentificationType;
import com.skyflow.detect.ObjectEntity;
import com.skyflow.detect.ObjectEntityType;
import com.skyflow.detect.OutputTranscriptionType;
import com.skyflow.detect.Pdf;
import com.skyflow.detect.PdfProcessingMode;
import com.skyflow.detect.ShiftDates;
import com.skyflow.detect.TestRedactionLevel;
import com.skyflow.detect.Transformation;
import com.skyflow.enums.RedactionType;
import com.skyflow.generated.common.rest.core.ObjectMappers;
import com.skyflow.generated.common.rest.types.AudioV2;
import com.skyflow.generated.common.rest.types.DetectConfigV2;
import com.skyflow.generated.common.rest.types.DetectCustomTypes;
import com.skyflow.generated.common.rest.types.DetectObjectEntity;
import com.skyflow.generated.common.rest.types.DocumentV2;
import com.skyflow.generated.common.rest.types.EntityTransformation;
import com.skyflow.generated.common.rest.types.ImageV2;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Field-level coverage of the inline-configuration mapping: object entities, file mappings, media
 * (audio, document, image), transformations and the optional-field branches of each.
 */
public class DetectConfigurationMapperTests {

    private static String json(Object value) throws Exception {
        return ObjectMappers.JSON_MAPPER.writeValueAsString(value);
    }

    // ---- object entities ----

    @Test
    public void everyObjectEntityTypeAndDeidentificationTypeMaps() {
        for (ObjectEntityType type : ObjectEntityType.values()) {
            DetectObjectEntity mapped = DetectRequestMapper.toObjectEntity(ObjectEntity.builder().entityType(type).build());
            Assert.assertEquals(type.name(), mapped.getEntityType().get().toString());
            Assert.assertFalse(mapped.getDeidentificationType().isPresent());
        }
        for (ObjectDeidentificationType type : ObjectDeidentificationType.values()) {
            DetectObjectEntity mapped = DetectRequestMapper.toObjectEntity(
                    ObjectEntity.builder().deidentificationType(type).build());
            Assert.assertEquals(type.name(), mapped.getDeidentificationType().get().toString());
            Assert.assertFalse(mapped.getEntityType().isPresent());
        }
    }

    @Test
    public void objectEntitiesAreCarriedInsideDetect() throws Exception {
        Detect detect = Detect.builder()
                .objectEntities(Arrays.asList(
                        ObjectEntity.builder().entityType(ObjectEntityType.FACE)
                                .deidentificationType(ObjectDeidentificationType.REDACT).build(),
                        ObjectEntity.builder().entityType(ObjectEntityType.LICENSE_PLATE)
                                .deidentificationType(ObjectDeidentificationType.UNREDACT).build()))
                .build();
        String json = json(DetectRequestMapper.toDetect(detect));
        Assert.assertEquals("{\"objectEntities\":[{\"entityType\":\"FACE\",\"deidentificationType\":\"REDACT\"},"
                + "{\"entityType\":\"LICENSE_PLATE\",\"deidentificationType\":\"UNREDACT\"}]}", json);
    }

    @Test
    public void emptyObjectEntityAndEmptyDetectSerializeToEmptyObjects() throws Exception {
        Assert.assertEquals("{}", json(DetectRequestMapper.toObjectEntity(ObjectEntity.builder().build())));
        Assert.assertEquals("{}", json(DetectRequestMapper.toDetect(Detect.builder().build())));
    }

    // ---- file mappings ----

    @Test
    public void fileMappingCarriesEveryField() throws Exception {
        List<com.skyflow.generated.common.rest.types.FileMapping> mapped = DetectRequestMapper.toFileMappings(
                Collections.singletonList(FileMapping.builder()
                        .source("documents.original")
                        .destination("documents.redacted")
                        .outputFileNameSuffix("-redacted")
                        .entitiesDestination("documents.entities_json")
                        .objectEntitiesDestination("documents.object_entities_json")
                        .build()));
        Assert.assertEquals(1, mapped.size());
        Assert.assertEquals("{\"source\":\"documents.original\",\"destination\":\"documents.redacted\","
                + "\"outputFileNameSuffix\":\"-redacted\",\"entitiesDestination\":\"documents.entities_json\","
                + "\"objectEntitiesDestination\":\"documents.object_entities_json\"}", json(mapped.get(0)));
    }

    @Test
    public void fileMappingOmitsUnsetFieldsAndSkipsNullElements() throws Exception {
        List<com.skyflow.generated.common.rest.types.FileMapping> mapped = DetectRequestMapper.toFileMappings(
                Arrays.asList(null, FileMapping.builder().build(), FileMapping.builder().destination("d").build()));
        Assert.assertEquals(2, mapped.size());
        Assert.assertEquals("{}", json(mapped.get(0)));
        Assert.assertEquals("{\"destination\":\"d\"}", json(mapped.get(1)));
        Assert.assertTrue(DetectRequestMapper.toFileMappings(Collections.<FileMapping>emptyList()).isEmpty());
    }

    // ---- media: audio ----

    @Test
    public void audioCarriesEveryFieldIncludingBleep() throws Exception {
        AudioV2 mapped = DetectRequestMapper.toAudio(Audio.builder()
                .outputProcessedAudio(true)
                .outputTranscription(OutputTranscriptionType.DIARIZED_TRANSCRIPTION)
                .bleep(Bleep.builder().startPadding(0.5f).stopPadding(0.25f).frequency(800).gain(-6).build())
                .build());
        Assert.assertEquals("{\"outputProcessedAudio\":true,\"outputTranscription\":\"DIARIZED_TRANSCRIPTION\","
                + "\"bleep\":{\"startPadding\":0.5,\"stopPadding\":0.25,\"frequency\":800,\"gain\":-6}}", json(mapped));
    }

    @Test
    public void everyOutputTranscriptionTypeMaps() {
        for (OutputTranscriptionType type : OutputTranscriptionType.values()) {
            AudioV2 mapped = DetectRequestMapper.toAudio(Audio.builder().outputTranscription(type).build());
            Assert.assertEquals(type.name(), mapped.getOutputTranscription().get().toString());
        }
    }

    @Test
    public void audioOmitsUnsetFieldsAndPartialBleep() throws Exception {
        Assert.assertEquals("{}", json(DetectRequestMapper.toAudio(Audio.builder().build())));
        Assert.assertEquals("{\"outputProcessedAudio\":false,\"bleep\":{}}",
                json(DetectRequestMapper.toAudio(Audio.builder().outputProcessedAudio(false).bleep(Bleep.builder().build()).build())));
        Assert.assertEquals("{\"bleep\":{\"startPadding\":1.0,\"gain\":0}}",
                json(DetectRequestMapper.toAudio(Audio.builder().bleep(Bleep.builder().startPadding(1f).gain(0).build()).build())));
        Assert.assertEquals("{\"bleep\":{\"stopPadding\":2.0,\"frequency\":600}}",
                json(DetectRequestMapper.toAudio(Audio.builder().bleep(Bleep.builder().stopPadding(2f).frequency(600).build()).build())));
    }

    // ---- media: document ----

    @Test
    public void documentCarriesPdfOptions() throws Exception {
        DocumentV2 mapped = DetectRequestMapper.toDocument(Document.builder()
                .pdf(Pdf.builder().processingMode(PdfProcessingMode.OCR).density(300).maxResolution(2048).build())
                .build());
        Assert.assertEquals("{\"pdf\":{\"processingMode\":\"OCR\",\"density\":300,\"maxResolution\":2048}}", json(mapped));
    }

    @Test
    public void everyPdfProcessingModeMaps() {
        for (PdfProcessingMode mode : PdfProcessingMode.values()) {
            DocumentV2 mapped = DetectRequestMapper.toDocument(
                    Document.builder().pdf(Pdf.builder().processingMode(mode).build()).build());
            Assert.assertEquals(mode.name(), mapped.getPdf().get().getProcessingMode().get().toString());
        }
    }

    @Test
    public void documentOmitsUnsetPdfFields() throws Exception {
        Assert.assertEquals("{}", json(DetectRequestMapper.toDocument(Document.builder().build())));
        Assert.assertEquals("{\"pdf\":{}}", json(DetectRequestMapper.toDocument(Document.builder().pdf(Pdf.builder().build()).build())));
        Assert.assertEquals("{\"pdf\":{\"density\":150}}",
                json(DetectRequestMapper.toDocument(Document.builder().pdf(Pdf.builder().density(150).build()).build())));
        Assert.assertEquals("{\"pdf\":{\"maxResolution\":1024}}",
                json(DetectRequestMapper.toDocument(Document.builder().pdf(Pdf.builder().maxResolution(1024).build()).build())));
    }

    // ---- media: image ----

    @Test
    public void imageCarriesEveryFieldAndEveryMaskingMethod() throws Exception {
        ImageV2 mapped = DetectRequestMapper.toImage(Image.builder()
                .outputProcessedImage(true).outputOcrText(false).maskingMethod(MaskingMethod.BLACKBOX).build());
        Assert.assertEquals("{\"outputProcessedImage\":true,\"outputOcrText\":false,\"maskingMethod\":\"BLACKBOX\"}", json(mapped));
        for (MaskingMethod method : MaskingMethod.values()) {
            Assert.assertEquals(method.name(), DetectRequestMapper.toImage(
                    Image.builder().maskingMethod(method).build()).getMaskingMethod().get().toString());
        }
        Assert.assertEquals("{}", json(DetectRequestMapper.toImage(Image.builder().build())));
        Assert.assertEquals("{\"outputProcessedImage\":false}",
                json(DetectRequestMapper.toImage(Image.builder().outputProcessedImage(false).build())));
    }

    // ---- media container ----

    @Test
    public void mediaCarriesOnlyTheSectionsThatAreSet() throws Exception {
        Assert.assertEquals("{}", json(DetectRequestMapper.toMedia(Media.builder().build())));
        Assert.assertEquals("{\"audio\":{\"outputProcessedAudio\":true}}",
                json(DetectRequestMapper.toMedia(Media.builder().audio(Audio.builder().outputProcessedAudio(true).build()).build())));
        Assert.assertEquals("{\"document\":{\"pdf\":{\"processingMode\":\"TEXT_LAYER\"}}}",
                json(DetectRequestMapper.toMedia(Media.builder()
                        .document(Document.builder().pdf(Pdf.builder().processingMode(PdfProcessingMode.TEXT_LAYER).build()).build())
                        .build())));
        Assert.assertEquals("{\"image\":{\"outputOcrText\":true}}",
                json(DetectRequestMapper.toMedia(Media.builder().image(Image.builder().outputOcrText(true).build()).build())));

        String all = json(DetectRequestMapper.toMedia(Media.builder()
                .audio(Audio.builder().outputProcessedAudio(true).build())
                .document(Document.builder().pdf(Pdf.builder().density(72).build()).build())
                .image(Image.builder().maskingMethod(MaskingMethod.BLUR).build())
                .build()));
        Assert.assertEquals("{\"audio\":{\"outputProcessedAudio\":true},\"document\":{\"pdf\":{\"density\":72}},"
                + "\"image\":{\"maskingMethod\":\"BLUR\"}}", all);
    }

    // ---- transformation ----

    @Test
    public void transformationOmitsUnsetShiftDateBounds() throws Exception {
        Assert.assertEquals("{}", json(DetectRequestMapper.toTransformation(Transformation.builder().build())));
        Assert.assertEquals("{\"shiftDates\":{}}",
                json(DetectRequestMapper.toTransformation(Transformation.builder().shiftDates(ShiftDates.builder().build()).build())));
        Assert.assertEquals("{\"shiftDates\":{\"minDays\":1}}",
                json(DetectRequestMapper.toTransformation(Transformation.builder().shiftDates(ShiftDates.builder().minDays(1).build()).build())));
        Assert.assertEquals("{\"shiftDates\":{\"maxDays\":9}}",
                json(DetectRequestMapper.toTransformation(Transformation.builder().shiftDates(ShiftDates.builder().maxDays(9).build()).build())));
        EntityTransformation full = DetectRequestMapper.toTransformation(
                Transformation.builder().shiftDates(ShiftDates.builder().minDays(-5).maxDays(5).build()).build());
        Assert.assertEquals(Integer.valueOf(-5), full.getShiftDates().get().getMinDays().get());
        Assert.assertEquals(Integer.valueOf(5), full.getShiftDates().get().getMaxDays().get());
    }

    // ---- custom types ----

    @Test
    public void customTypeCarriesEveryVaultTokenField() throws Exception {
        DetectCustomTypes mapped = DetectRequestMapper.toCustomType(CustomType.builder()
                .label("PRODUCT")
                .match(Arrays.asList("PRD-[0-9]{4}", "SKU-[A-Z]{3}"))
                .deidentificationType(DeidentificationType.VAULT_TOKEN)
                .destination("productTokenGroup")
                .includePrefix(IncludePrefix.YES)
                .prefixOverride("PRODUKT")
                .build());
        Assert.assertEquals("{\"label\":\"PRODUCT\",\"match\":[\"PRD-[0-9]{4}\",\"SKU-[A-Z]{3}\"],"
                + "\"deidentificationType\":\"VAULT_TOKEN\",\"destination\":\"productTokenGroup\","
                + "\"includePrefix\":\"YES\",\"prefixOverride\":\"PRODUKT\"}", json(mapped));
        Assert.assertEquals("{}", json(DetectRequestMapper.toCustomType(CustomType.builder().build())));
    }

    // ---- configuration container ----

    @Test
    public void configurationCarriesDescriptionFileMappingAndMedia() throws Exception {
        DetectConfigV2 mapped = DetectRequestMapper.toDetectConfig(DetectConfiguration.builder()
                .name("files")
                .description("inline file policy")
                .fileMapping(Collections.singletonList(FileMapping.builder().source("docs.in").destination("docs.out").build()))
                .media(Media.builder().image(Image.builder().outputProcessedImage(true).build()).build())
                .build(), "vault-9");
        Assert.assertEquals("inline file policy", mapped.getDescription().get());
        Assert.assertFalse(mapped.getDetect().isPresent());
        Assert.assertEquals("{\"name\":\"files\",\"description\":\"inline file policy\",\"vaultId\":\"vault-9\","
                + "\"fileMapping\":[{\"source\":\"docs.in\",\"destination\":\"docs.out\"}],"
                + "\"media\":{\"image\":{\"outputProcessedImage\":true}}}", json(mapped));
    }

    @Test
    public void nullConfigurationYieldsVaultIdOnly() throws Exception {
        Assert.assertEquals("{\"vaultId\":\"v\"}", json(DetectRequestMapper.toDetectConfig(null, "v")));
        Assert.assertEquals("{\"vaultId\":\"v\"}", json(DetectRequestMapper.toDetectConfig(DetectConfiguration.builder().build(), "v")));
    }

    @Test
    public void detectWithoutEntitiesStillCarriesRestrictSkipAndObjectEntities() throws Exception {
        String json = json(DetectRequestMapper.toDetect(Detect.builder()
                .objectEntities(Collections.singletonList(ObjectEntity.builder().entityType(ObjectEntityType.LOGO).build()))
                .restrict(Collections.singletonList("A-[0-9]+"))
                .skip(Collections.singletonList("ACME"))
                .build()));
        Assert.assertEquals("{\"objectEntities\":[{\"entityType\":\"LOGO\"}],\"restrict\":[\"A-[0-9]+\"],\"skip\":[\"ACME\"]}", json);
        Assert.assertFalse(json.contains("entities\":[]"));
    }

    // ---- small helpers ----

    @Test
    public void inferFormatHandlesNullNoExtensionTrailingDotAndUnknown() {
        Assert.assertNull(DetectRequestMapper.inferFormat(null));
        Assert.assertNull(DetectRequestMapper.inferFormat("README"));
        Assert.assertNull(DetectRequestMapper.inferFormat("archive."));
        Assert.assertNull(DetectRequestMapper.inferFormat("notes.unknownext"));
        Assert.assertEquals(DeidentifyFileDataFormat.PDF, DetectRequestMapper.inferFormat("scan.PDF"));
        Assert.assertEquals(DeidentifyFileDataFormat.TXT, DetectRequestMapper.inferFormat("/tmp/a.b/notes.txt"));
    }

    @Test
    public void redactionLevelsSkipNullElements() {
        List<com.skyflow.generated.common.rest.types.RedactionLevel> mapped = DetectRequestMapper.toRedactionLevels(
                Arrays.asList(null, TestRedactionLevel.builder().entityName(EntityType.SSN).redactionType(RedactionType.REDACTED).build(), null));
        Assert.assertEquals(1, mapped.size());
        Assert.assertEquals("SSN", mapped.get(0).getEntityName().get().toString());
        Assert.assertEquals("REDACTED", mapped.get(0).getRedactionType().get().toString());
    }
}
