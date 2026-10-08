package com.skyflow.detect.internal;

import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.CustomType;
import com.skyflow.detect.DeidentificationType;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.Detect;
import com.skyflow.detect.DetectConfiguration;
import com.skyflow.detect.Entity;
import com.skyflow.detect.EntityType;
import com.skyflow.detect.Image;
import com.skyflow.detect.MaskingMethod;
import com.skyflow.detect.Media;
import com.skyflow.detect.RedactionLevel;
import com.skyflow.detect.RedactionType;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.detect.ReturnEntitiesType;
import com.skyflow.detect.ShiftDates;
import com.skyflow.detect.Transformation;
import com.skyflow.generated.detect.rest.core.ObjectMappers;
import com.skyflow.generated.detect.rest.resources.guardrailsv2.requests.DetectGuardrailsRequestV2;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.DeidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.resources.stringsv2.requests.ReidentifyStringRequestV2;
import com.skyflow.generated.detect.rest.types.DetectConfigV2;
import com.skyflow.detect.FileMapping;
import com.skyflow.detect.ObjectEntity;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class DetectRequestMapperTests {

    @Test
    public void storedConfigurationSendsOnlyTextAndId() throws Exception {
        DeidentifyStringRequestV2 mapped = DetectRequestMapper.toDeidentifyStringRequest(
                DeidentifyStringRequest.builder().text("hello").configurationId("cfg-1").build(), "vault-1");
        Assert.assertEquals("hello", mapped.getText());
        Assert.assertEquals("cfg-1", mapped.getConfigurationId().get());
        Assert.assertFalse(mapped.getConfiguration().isPresent());
        String json = ObjectMappers.JSON_MAPPER.writeValueAsString(mapped);
        Assert.assertTrue(json, json.contains("\"configurationId\":\"cfg-1\""));
        Assert.assertFalse("no vault id outside a configuration: " + json, json.contains("vaultId"));
        Assert.assertFalse(json, json.contains("\"configuration\":"));
    }

    @Test
    public void inlineConfigurationIsScopedToVaultAndFullyMapped() throws Exception {
        DetectConfiguration configuration = DetectConfiguration.builder()
                .name("inline")
                .detect(Detect.builder()
                        .entities(Arrays.asList(
                                Entity.builder().entityType(EntityType.NAME)
                                        .deidentificationType(DeidentificationType.VAULT_TOKEN)
                                        .destination("persons.name").build(),
                                Entity.builder().entityType(EntityType.DOB)
                                        .deidentificationType(DeidentificationType.ENTITY_UNIQUE_COUNTER)
                                        .transformation(Transformation.builder()
                                                .shiftDates(ShiftDates.builder().minDays(10).maxDays(30).build()).build())
                                        .build()))
                        .restrict(Collections.singletonList("[0-9]{3}-[0-9]{2}-[0-9]{4}"))
                        .skip(Collections.singletonList("Skyflow"))
                        .customTypes(Collections.singletonList(
                                CustomType.builder().label("EMPLOYEE_ID").match(Collections.singletonList("EMP-[0-9]{6}")).build()))
                        .returnEntities(ReturnEntitiesType.ALL)
                        .build())
                .media(Media.builder().image(Image.builder().maskingMethod(MaskingMethod.BLUR).outputOcrText(true).build()).build())
                .build();

        DeidentifyStringRequestV2 mapped = DetectRequestMapper.toDeidentifyStringRequest(
                DeidentifyStringRequest.builder().text("hello").configuration(configuration).build(), "vault-1");

        DetectConfigV2 config = mapped.getConfiguration().get();
        Assert.assertEquals("vault-1", config.getVaultId());
        Assert.assertEquals("inline", config.getName().get());
        Assert.assertFalse(mapped.getConfigurationId().isPresent());

        String json = ObjectMappers.JSON_MAPPER.writeValueAsString(mapped);
        Assert.assertEquals("vault id appears once, inside the inline configuration",
                1, json.split("\"vaultId\":\"vault-1\"", -1).length - 1);
        for (String expected : new String[]{
                "\"vaultId\":\"vault-1\"", "\"entityType\":\"NAME\"", "\"deidentificationType\":\"VAULT_TOKEN\"",
                "\"destination\":\"persons.name\"", "\"minDays\":10", "\"maxDays\":30",
                "\"restrict\":[\"[0-9]{3}-[0-9]{2}-[0-9]{4}\"]", "\"skip\":[\"Skyflow\"]",
                "\"label\":\"EMPLOYEE_ID\"", "\"returnEntities\":\"ALL\"", "\"maskingMethod\":\"BLUR\"", "\"outputOcrText\":true"}) {
            Assert.assertTrue("missing " + expected + " in " + json, json.contains(expected));
        }
        Assert.assertFalse("unset fields must be omitted: " + json, json.contains("\"description\""));
    }

    @Test
    public void reidentifyStringCarriesVaultIdAndRedactionLevels() throws Exception {
        ReidentifyStringRequestV2 mapped = DetectRequestMapper.toReidentifyStringRequest(
                ReidentifyStringRequest.builder()
                        .text("[NAME_1]")
                        .redactionLevel(Arrays.asList(
                                RedactionLevel.builder().entityName(EntityType.NAME).redactionType(RedactionType.MASKED).build(),
                                RedactionLevel.builder().tokenGroupName("group").redactionPattern("pattern").build()))
                        .build(), "vault-1");
        Assert.assertEquals("vault-1", mapped.getVaultId());
        Assert.assertEquals("[NAME_1]", mapped.getText());
        Assert.assertEquals(2, mapped.getRedactionLevel().get().size());
        String json = ObjectMappers.JSON_MAPPER.writeValueAsString(mapped);
        Assert.assertTrue(json, json.contains("\"entityName\":\"NAME\""));
        Assert.assertTrue(json, json.contains("\"redactionType\":\"MASKED\""));
        Assert.assertTrue(json, json.contains("\"tokenGroupName\":\"group\""));
        Assert.assertTrue(json, json.contains("\"redactionPattern\":\"pattern\""));
    }

    @Test
    public void reidentifyStringWithoutRedactionLevelsOmitsField() throws Exception {
        ReidentifyStringRequestV2 mapped = DetectRequestMapper.toReidentifyStringRequest(
                ReidentifyStringRequest.builder().text("[NAME_1]").build(), "vault-1");
        Assert.assertFalse(mapped.getRedactionLevel().isPresent());
        Assert.assertFalse(ObjectMappers.JSON_MAPPER.writeValueAsString(mapped).contains("redactionLevel"));
    }

    @Test
    public void everyPublicEntityTypeMapsToAGeneratedValue() {
        for (EntityType type : EntityType.values()) {
            Entity entity = Entity.builder().entityType(type).build();
            Assert.assertEquals(type.name(), DetectRequestMapper.toEntity(entity).getEntityType().get().toString());
            RedactionLevel level = RedactionLevel.builder().entityName(type).redactionType(RedactionType.DEFAULT).build();
            Assert.assertEquals(type.name(),
                    DetectRequestMapper.toRedactionLevels(Collections.singletonList(level)).get(0).getEntityName().get().toString());
        }
    }

    @Test
    public void checkGuardrailsCarriesVaultIdAndOnlySetOptions() throws Exception {
        DetectGuardrailsRequestV2 full = DetectRequestMapper.toCheckGuardrailsRequest(
                CheckGuardrailsRequest.builder().text("t").checkToxicity(false).denyTopics(Collections.singletonList("x")).build(), "vault-1");
        Assert.assertEquals("vault-1", full.getVaultId());
        Assert.assertEquals("t", full.getText());
        Assert.assertEquals(Boolean.FALSE, full.getCheckToxicity().get());
        Assert.assertEquals(Collections.singletonList("x"), full.getDenyTopics().get());

        DetectGuardrailsRequestV2 minimal = DetectRequestMapper.toCheckGuardrailsRequest(
                CheckGuardrailsRequest.builder().text("t").build(), "vault-1");
        Assert.assertFalse(minimal.getCheckToxicity().isPresent());
        Assert.assertFalse(minimal.getDenyTopics().isPresent());
    }

    @Test
    public void nullElementsInConfigurationListsAreSkipped() throws Exception {
        com.skyflow.generated.detect.rest.types.DetectConfigV2 api = DetectRequestMapper.toDetectConfig(
                DetectConfiguration.builder()
                        .detect(Detect.builder()
                                .entities(Arrays.asList(null, Entity.builder().entityType(EntityType.NAME).build()))
                                .objectEntities(Arrays.<ObjectEntity>asList((ObjectEntity) null))
                                .customTypes(Arrays.asList(null, CustomType.builder().label("ID").build()))
                                .build())
                        .fileMapping(Arrays.asList(null, FileMapping.builder().source("in").build()))
                        .build(), "v");
        Assert.assertEquals(1, api.getDetect().get().getEntities().get().size());
        Assert.assertTrue(api.getDetect().get().getObjectEntities().get().isEmpty());
        Assert.assertEquals(1, api.getDetect().get().getCustomTypes().get().size());
        Assert.assertEquals(1, api.getFileMapping().get().size());
    }
}
