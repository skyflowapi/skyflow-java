package com.skyflow.detect;

import com.skyflow.detect.internal.DetectResponseMapper;
import com.skyflow.generated.common.rest.core.ObjectMappers;
import com.skyflow.generated.common.rest.types.DeidentifyStringResponseV2;
import com.skyflow.generated.common.rest.types.DetectGuardrailsResponseV2;
import com.skyflow.generated.common.rest.types.ReidentifyStringResponseV2;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Getter and toString coverage of the public detect models. toString must mention every field so a logged
 * configuration is diagnosable, and must never expose a raw file payload.
 */
public class DetectModelsTests {

    private static void assertMentions(Object model, String... fragments) {
        String text = model.toString();
        Assert.assertTrue(text, text.startsWith(model.getClass().getSimpleName() + "{"));
        for (String fragment : fragments) {
            Assert.assertTrue("expected '" + fragment + "' in " + text, text.contains(fragment));
        }
    }

    @Test
    public void entityExposesEveryFieldAndDefaultsToNull() {
        Entity empty = Entity.builder().build();
        Assert.assertNull(empty.getEntityType());
        Assert.assertNull(empty.getDeidentificationType());
        Assert.assertNull(empty.getDestination());
        Assert.assertNull(empty.getTransformation());
        Assert.assertNull(empty.getIncludePrefix());
        Assert.assertNull(empty.getPrefixOverride());

        Transformation transformation = Transformation.builder()
                .shiftDates(ShiftDates.builder().minDays(1).maxDays(2).build()).build();
        Entity entity = Entity.builder()
                .entityType(EntityType.NAME)
                .deidentificationType(DeidentificationType.VAULT_TOKEN)
                .destination("persons.name")
                .transformation(transformation)
                .includePrefix(IncludePrefix.NO)
                .prefixOverride("NAAM")
                .build();
        Assert.assertEquals(EntityType.NAME, entity.getEntityType());
        Assert.assertEquals(DeidentificationType.VAULT_TOKEN, entity.getDeidentificationType());
        Assert.assertEquals("persons.name", entity.getDestination());
        Assert.assertSame(transformation, entity.getTransformation());
        Assert.assertEquals(IncludePrefix.NO, entity.getIncludePrefix());
        Assert.assertEquals("NAAM", entity.getPrefixOverride());
        assertMentions(entity, "entityType=NAME", "deidentificationType=VAULT_TOKEN", "destination=persons.name",
                "includePrefix=NO", "prefixOverride=NAAM", "minDays=1", "maxDays=2");
        assertMentions(transformation, "shiftDates=");
        assertMentions(transformation.getShiftDates(), "minDays=1", "maxDays=2");
    }

    @Test
    public void customTypeExposesEveryFieldAndDefaultsToNull() {
        CustomType empty = CustomType.builder().build();
        Assert.assertNull(empty.getLabel());
        Assert.assertNull(empty.getMatch());
        Assert.assertNull(empty.getDeidentificationType());
        Assert.assertNull(empty.getDestination());
        Assert.assertNull(empty.getIncludePrefix());
        Assert.assertNull(empty.getPrefixOverride());

        CustomType custom = CustomType.builder()
                .label("EMPLOYEE_ID")
                .match(Arrays.asList("EMP-[0-9]{6}", "E[0-9]{8}"))
                .deidentificationType(DeidentificationType.ENTITY_ONLY)
                .destination("employees.id")
                .includePrefix(IncludePrefix.INCLUDE_PREFIX_UNSPECIFIED)
                .prefixOverride("EMP")
                .build();
        Assert.assertEquals("EMPLOYEE_ID", custom.getLabel());
        Assert.assertEquals(2, custom.getMatch().size());
        Assert.assertEquals(DeidentificationType.ENTITY_ONLY, custom.getDeidentificationType());
        Assert.assertEquals("employees.id", custom.getDestination());
        Assert.assertEquals(IncludePrefix.INCLUDE_PREFIX_UNSPECIFIED, custom.getIncludePrefix());
        Assert.assertEquals("EMP", custom.getPrefixOverride());
        assertMentions(custom, "label=EMPLOYEE_ID", "match=[EMP-[0-9]{6}, E[0-9]{8}]", "deidentificationType=ENTITY_ONLY",
                "destination=employees.id", "includePrefix=INCLUDE_PREFIX_UNSPECIFIED", "prefixOverride=EMP");
    }

    @Test
    public void includePrefixEnumMatchesContract() {
        Assert.assertArrayEquals(new IncludePrefix[]{IncludePrefix.INCLUDE_PREFIX_UNSPECIFIED, IncludePrefix.YES, IncludePrefix.NO},
                IncludePrefix.values());
        Assert.assertEquals(IncludePrefix.YES, IncludePrefix.valueOf("YES"));
    }

    @Test
    public void objectEntityAndItsEnums() {
        ObjectEntity objectEntity = ObjectEntity.builder()
                .entityType(ObjectEntityType.SIGNATURE)
                .deidentificationType(ObjectDeidentificationType.REDACT)
                .build();
        Assert.assertEquals(ObjectEntityType.SIGNATURE, objectEntity.getEntityType());
        Assert.assertEquals(ObjectDeidentificationType.REDACT, objectEntity.getDeidentificationType());
        assertMentions(objectEntity, "entityType=SIGNATURE", "deidentificationType=REDACT");
        Assert.assertNull(ObjectEntity.builder().build().getEntityType());
        Assert.assertEquals(6, ObjectEntityType.values().length);
        Assert.assertEquals(ObjectEntityType.ALL, ObjectEntityType.valueOf("ALL"));
        Assert.assertEquals(3, ObjectDeidentificationType.values().length);
        Assert.assertEquals(ObjectDeidentificationType.UNREDACT, ObjectDeidentificationType.valueOf("UNREDACT"));
    }

    @Test
    public void mediaModelsExposeEveryField() {
        Bleep bleep = Bleep.builder().startPadding(0.5f).stopPadding(0.2f).frequency(600).gain(-3).build();
        Assert.assertEquals(Float.valueOf(0.5f), bleep.getStartPadding());
        Assert.assertEquals(Float.valueOf(0.2f), bleep.getStopPadding());
        Assert.assertEquals(Integer.valueOf(600), bleep.getFrequency());
        Assert.assertEquals(Integer.valueOf(-3), bleep.getGain());
        assertMentions(bleep, "startPadding=0.5", "stopPadding=0.2", "frequency=600", "gain=-3");

        Audio audio = Audio.builder().outputProcessedAudio(true)
                .outputTranscription(OutputTranscriptionType.MEDICAL_TRANSCRIPTION).bleep(bleep).build();
        Assert.assertEquals(Boolean.TRUE, audio.getOutputProcessedAudio());
        Assert.assertEquals(OutputTranscriptionType.MEDICAL_TRANSCRIPTION, audio.getOutputTranscription());
        Assert.assertSame(bleep, audio.getBleep());
        assertMentions(audio, "outputProcessedAudio=true", "outputTranscription=MEDICAL_TRANSCRIPTION", "bleep=Bleep{");
        Assert.assertNull(Audio.builder().build().getBleep());

        Pdf pdf = Pdf.builder().processingMode(PdfProcessingMode.OCR).density(300).maxResolution(4096).build();
        Assert.assertEquals(PdfProcessingMode.OCR, pdf.getProcessingMode());
        Assert.assertEquals(Integer.valueOf(300), pdf.getDensity());
        Assert.assertEquals(Integer.valueOf(4096), pdf.getMaxResolution());
        assertMentions(pdf, "processingMode=OCR", "density=300", "maxResolution=4096");

        Document document = Document.builder().pdf(pdf).build();
        Assert.assertSame(pdf, document.getPdf());
        assertMentions(document, "pdf=Pdf{");
        Assert.assertNull(Document.builder().build().getPdf());

        Image image = Image.builder().outputProcessedImage(false).outputOcrText(true).maskingMethod(MaskingMethod.BLACKBOX).build();
        Assert.assertEquals(Boolean.FALSE, image.getOutputProcessedImage());
        Assert.assertEquals(Boolean.TRUE, image.getOutputOcrText());
        Assert.assertEquals(MaskingMethod.BLACKBOX, image.getMaskingMethod());
        assertMentions(image, "outputProcessedImage=false", "outputOcrText=true", "maskingMethod=BLACKBOX");

        Media media = Media.builder().audio(audio).document(document).image(image).build();
        Assert.assertSame(audio, media.getAudio());
        Assert.assertSame(document, media.getDocument());
        Assert.assertSame(image, media.getImage());
        assertMentions(media, "audio=Audio{", "document=Document{", "image=Image{");

        Assert.assertEquals(5, OutputTranscriptionType.values().length);
        Assert.assertEquals(OutputTranscriptionType.NONE, OutputTranscriptionType.valueOf("NONE"));
        Assert.assertEquals(3, PdfProcessingMode.values().length);
        Assert.assertEquals(PdfProcessingMode.TEXT_LAYER, PdfProcessingMode.valueOf("TEXT_LAYER"));
    }

    @Test
    public void fileMappingExposesEveryField() {
        FileMapping mapping = FileMapping.builder()
                .source("docs.original")
                .destination("docs.redacted")
                .outputFileNameSuffix("_redacted")
                .entitiesDestination("docs.entities")
                .objectEntitiesDestination("docs.object_entities")
                .build();
        Assert.assertEquals("docs.original", mapping.getSource());
        Assert.assertEquals("docs.redacted", mapping.getDestination());
        Assert.assertEquals("_redacted", mapping.getOutputFileNameSuffix());
        Assert.assertEquals("docs.entities", mapping.getEntitiesDestination());
        Assert.assertEquals("docs.object_entities", mapping.getObjectEntitiesDestination());
        assertMentions(mapping, "source=docs.original", "destination=docs.redacted", "outputFileNameSuffix=_redacted",
                "entitiesDestination=docs.entities", "objectEntitiesDestination=docs.object_entities");
    }

    @Test
    public void configurationContainersExposeEveryField() {
        Detect detect = Detect.builder()
                .entities(Collections.singletonList(Entity.builder().entityType(EntityType.SSN).build()))
                .objectEntities(Collections.singletonList(ObjectEntity.builder().entityType(ObjectEntityType.FACE).build()))
                .restrict(Collections.singletonList("r"))
                .skip(Collections.singletonList("s"))
                .customTypes(Collections.singletonList(CustomType.builder().label("L").build()))
                .returnEntities(ReturnEntitiesType.NONE)
                .build();
        Assert.assertEquals(1, detect.getEntities().size());
        Assert.assertEquals(1, detect.getObjectEntities().size());
        Assert.assertEquals(Collections.singletonList("r"), detect.getRestrict());
        Assert.assertEquals(Collections.singletonList("s"), detect.getSkip());
        Assert.assertEquals(1, detect.getCustomTypes().size());
        Assert.assertEquals(ReturnEntitiesType.NONE, detect.getReturnEntities());
        assertMentions(detect, "entities=[Entity{", "objectEntities=[ObjectEntity{", "restrict=[r]", "skip=[s]",
                "customTypes=[CustomType{", "returnEntities=NONE");

        Media media = Media.builder().build();
        FileMapping mapping = FileMapping.builder().source("a").build();
        DetectConfiguration configuration = DetectConfiguration.builder()
                .name("n").description("d").detect(detect).fileMapping(Collections.singletonList(mapping)).media(media).build();
        Assert.assertEquals("n", configuration.getName());
        Assert.assertEquals("d", configuration.getDescription());
        Assert.assertSame(detect, configuration.getDetect());
        Assert.assertSame(mapping, configuration.getFileMapping().get(0));
        Assert.assertSame(media, configuration.getMedia());
        assertMentions(configuration, "name=n", "description=d", "detect=Detect{", "fileMapping=[FileMapping{", "media=Media{");
    }

    @Test
    public void requestModelsExposeEveryField() {
        PollOptions poll = PollOptions.builder().waitTime(30).maxAttempts(4).build();
        Assert.assertEquals(Integer.valueOf(30), poll.getWaitTime());
        Assert.assertEquals(Integer.valueOf(4), poll.getMaxAttempts());
        assertMentions(poll, "waitTime=30", "maxAttempts=4");

        GetRunRequest run = GetRunRequest.builder().runId("run-1").build();
        Assert.assertEquals("run-1", run.getRunId());
        assertMentions(run, "runId=run-1");

        CheckGuardrailsRequest guardrails = CheckGuardrailsRequest.builder()
                .text("t").checkToxicity(true).denyTopics(Collections.singletonList("politics")).build();
        Assert.assertEquals("t", guardrails.getText());
        Assert.assertEquals(Boolean.TRUE, guardrails.getCheckToxicity());
        Assert.assertEquals(Collections.singletonList("politics"), guardrails.getDenyTopics());
        assertMentions(guardrails, "text=t", "checkToxicity=true", "denyTopics=[politics]");

        DeidentifyStringRequest deidentify = DeidentifyStringRequest.builder().text("hello").configurationId("cfg").build();
        assertMentions(deidentify, "text=hello", "configurationId=cfg");

        ReidentifyStringRequest reidentify = ReidentifyStringRequest.builder().text("[NAME_1]")
                .redactionLevel(Collections.singletonList(
                        TestRedactionLevel.builder().tokenGroupName("g").redactionPattern("p").build())).build();
        Assert.assertEquals(1, reidentify.getRedactionLevel().size());
        assertMentions(reidentify, "text=[NAME_1]", "redactionLevel=[");
    }

    @Test
    public void responseModelsExposeEveryFieldThroughToString() throws Exception {
        EntityLocation location = new EntityLocation(1, 5, 2, 9);
        Assert.assertEquals(Integer.valueOf(1), location.getStartIndex());
        Assert.assertEquals(Integer.valueOf(5), location.getEndIndex());
        Assert.assertEquals(Integer.valueOf(2), location.getStartIndexProcessed());
        Assert.assertEquals(Integer.valueOf(9), location.getEndIndexProcessed());
        assertMentions(location, "startIndex=1", "endIndex=5", "startIndexProcessed=2", "endIndexProcessed=9");

        DeidentifyStringResponse deidentified = DetectResponseMapper.toDeidentifyStringResponse(ObjectMappers.JSON_MAPPER.readValue(
                "{\"processedText\":\"Hi [NAME_1]\",\"entities\":[{\"token\":\"NAME_1\",\"value\":\"Jo\","
                        + "\"location\":{\"startIndex\":3,\"endIndex\":5},\"entityType\":\"NAME\",\"entityScores\":{\"NAME\":0.9}}],"
                        + "\"metrics\":{\"size\":1.5,\"wordCount\":2,\"characterCount\":5,\"slides\":0,\"pages\":1,\"duration\":0.1}}",
                DeidentifyStringResponseV2.class));
        assertMentions(deidentified, "processedText=Hi [NAME_1]", "entities=[DetectedEntity{", "metrics=Metrics{");
        assertMentions(deidentified.getEntities().get(0), "token=NAME_1", "value=Jo", "location=EntityLocation{",
                "entityType=NAME", "entityScores={NAME=0.9}");
        assertMentions(deidentified.getMetrics(), "size=1.5", "wordCount=2", "characterCount=5", "slides=0", "pages=1", "duration=0.1");

        ReidentifyStringResponse reidentified = DetectResponseMapper.toReidentifyStringResponse(ObjectMappers.JSON_MAPPER.readValue(
                "{\"processedText\":\"Hi Jo\"}", ReidentifyStringResponseV2.class));
        assertMentions(reidentified, "processedText=Hi Jo");

        CheckGuardrailsResponse guardrails = DetectResponseMapper.toCheckGuardrailsResponse(ObjectMappers.JSON_MAPPER.readValue(
                "{\"text\":\"t\",\"toxic\":true,\"deniedTopic\":false,\"validation\":\"FAILED\"}", DetectGuardrailsResponseV2.class));
        assertMentions(guardrails, "text=t", "toxic=true", "deniedTopic=false", "validation=FAILED");

        DeidentifyFileResponse submitted = DetectResponseMapper.submittedDeidentifyFileResponse("run-7");
        Assert.assertEquals("run-7", submitted.getRunId());
        Assert.assertNull(submitted.getStatus());
        Assert.assertTrue(submitted.getOutput().isEmpty());
        assertMentions(submitted, "runId=run-7", "status=null", "output=[]");

        FileOutput output = DetectResponseMapper.toFileOutput(ObjectMappers.JSON_MAPPER.readValue(
                "{\"processedFile\":\"QUJD\",\"processedFileType\":\"REDACTED_FILE\",\"processedFileExtension\":\"txt\"}",
                com.skyflow.generated.common.rest.types.FileOutput.class));
        Assert.assertEquals("REDACTED_FILE", output.getProcessedFileType());
        assertMentions(output, "processedFileType=REDACTED_FILE", "processedFileExtension=");
    }
}
