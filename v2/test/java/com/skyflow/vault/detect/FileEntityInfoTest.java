package com.skyflow.vault.detect;

import com.skyflow.generated.rest.types.DeidentifyFileOutputProcessedFileType;
import org.junit.Assert;
import org.junit.Test;

public class FileEntityInfoTest {

    @Test
    public void testConstructorAndGetters() {
        String file = "entity.pdf";
        DeidentifyFileOutputProcessedFileType type = DeidentifyFileOutputProcessedFileType.ENTITIES;
        String extension = ".pdf";

        FileEntityInfo info = new FileEntityInfo(file, type, extension);

        Assert.assertEquals(file, info.getFile());
        Assert.assertEquals(type.toString(), info.getType());
        Assert.assertEquals(extension, info.getExtension());
    }
}