package com.skyflow.vault.detect;

import com.skyflow.generated.common.rest.resources.filesv2.types.DeidentifyFileRequestV2DataFormat;
import com.skyflow.generated.common.rest.resources.filesv2.types.ReidentifyFileRequestV2DataFormat;
import com.skyflow.generated.common.rest.types.FileOutputProcessedFileExtension;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * The three public file-format enums must mirror the generated wire enums value for value, so a caller can never
 * select a format the corresponding endpoint rejects, and never miss one it accepts.
 */
public class DetectFileFormatEnumsTests {

    @Test
    public void deidentifyFileDataFormatMirrorsTheDeidentifyRequestWireEnum() {
        Set<String> wire = new TreeSet<>();
        for (DeidentifyFileRequestV2DataFormat.Value v : EnumSet.complementOf(EnumSet.of(DeidentifyFileRequestV2DataFormat.Value.UNKNOWN))) {
            wire.add(v.name());
        }
        Assert.assertEquals(wire, names(DeidentifyFileDataFormat.values()));
        for (DeidentifyFileDataFormat f : DeidentifyFileDataFormat.values()) {
            Assert.assertNotEquals(f.name(), DeidentifyFileRequestV2DataFormat.Value.UNKNOWN,
                    DeidentifyFileRequestV2DataFormat.valueOf(f.name().toLowerCase()).getEnumValue());
        }
    }

    @Test
    public void reidentifyFileDataFormatMirrorsTheReidentifyRequestWireEnum() {
        Set<String> wire = new TreeSet<>();
        for (ReidentifyFileRequestV2DataFormat.Value v : EnumSet.complementOf(EnumSet.of(ReidentifyFileRequestV2DataFormat.Value.UNKNOWN))) {
            wire.add(v.name());
        }
        Assert.assertEquals(wire, names(ReidentifyFileDataFormat.values()));
        Assert.assertEquals(8, ReidentifyFileDataFormat.values().length);
        for (ReidentifyFileDataFormat f : ReidentifyFileDataFormat.values()) {
            Assert.assertNotEquals(f.name(), ReidentifyFileRequestV2DataFormat.Value.UNKNOWN,
                    ReidentifyFileRequestV2DataFormat.valueOf(f.name().toLowerCase()).getEnumValue());
        }
    }

    @Test
    public void deidentifyFileDataFormatAlsoMirrorsTheOutputExtensionWireEnum() {
        Set<String> wire = new TreeSet<>();
        for (FileOutputProcessedFileExtension.Value v : EnumSet.complementOf(EnumSet.of(FileOutputProcessedFileExtension.Value.UNKNOWN))) {
            wire.add(v.name());
        }
        Assert.assertEquals(wire, names(DeidentifyFileDataFormat.values()));
    }

    @Test
    public void reidentifyFormatsAreASubsetOfDeidentifyFormats() {
        Set<String> deidentify = names(DeidentifyFileDataFormat.values());
        for (ReidentifyFileDataFormat f : ReidentifyFileDataFormat.values()) {
            Assert.assertTrue(f.name(), deidentify.contains(f.name()));
        }
    }

    private static Set<String> names(Enum<?>[] values) {
        Set<String> out = new TreeSet<>();
        for (Enum<?> e : values) {
            out.add(e.name());
        }
        Assert.assertEquals("no duplicates", values.length, out.size());
        return new TreeSet<>(Arrays.asList(out.toArray(new String[0])));
    }
}
