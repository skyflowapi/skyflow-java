package com.skyflow.vault.detect;

import com.skyflow.generated.rest.types.DeidentifiedFileOutputProcessedFileExtension;
import com.skyflow.generated.rest.types.DeidentifiedFileOutputProcessedFileType;
import org.junit.Assert;
import org.junit.Test;

public class FileEntityInfoTest {

    @Test
    public void testConstructorAndGetters() {
        String file = "entity.pdf";
        DeidentifiedFileOutputProcessedFileType type = DeidentifiedFileOutputProcessedFileType.ENTITIES;
        String extension = "pdf";

        FileEntityInfo info = new FileEntityInfo(file, type, DeidentifiedFileOutputProcessedFileExtension.PDF);

        Assert.assertEquals(file, info.getFile());
        Assert.assertEquals(type.toString(), info.getType());
        Assert.assertEquals(extension, info.getExtension());
    }
}