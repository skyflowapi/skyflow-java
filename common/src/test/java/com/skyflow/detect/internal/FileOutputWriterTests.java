package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.FileDataFormat;
import com.skyflow.detect.FileOutput;
import com.skyflow.errors.SkyflowException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

public class FileOutputWriterTests {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static String b64(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes());
    }

    @Test
    public void writesEachOutputWithV1StyleNames() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("%PDF"), "REDACTED_FILE", FileDataFormat.PDF),
                new FileOutput(b64("[]"), "ENTITIES", null)),
                DataSourceType.BASE64, "invoice.pdf", folder.getRoot().getPath());

        Assert.assertEquals(2, written.size());
        Assert.assertEquals("processed-invoice.pdf", written.get(0).getName());
        Assert.assertEquals("processed-entities-invoice.json", written.get(1).getName());
        Assert.assertEquals("%PDF", new String(Files.readAllBytes(written.get(0).toPath())));
        Assert.assertEquals("[]", new String(Files.readAllBytes(written.get(1).toPath())));
        Assert.assertEquals(folder.getRoot(), written.get(0).getParentFile());
    }

    @Test
    public void entityOutputsCarryTheirTypeInTheName() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), "ENTITIES", FileDataFormat.JSON),
                new FileOutput(b64("b"), "OBJECT_ENTITIES", FileDataFormat.JSON)),
                DataSourceType.BASE64, "scan.png", folder.getRoot().getPath());
        Assert.assertEquals("processed-entities-scan.json", written.get(0).getName());
        Assert.assertEquals("processed-object-entities-scan.json", written.get(1).getName());
    }

    @Test
    public void jsonInputDoesNotCollideWithItsEntitiesOutput() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("{}"), "REDACTED_TEXT", FileDataFormat.JSON),
                new FileOutput(b64("[]"), "ENTITIES", FileDataFormat.JSON)),
                DataSourceType.BASE64, "data.json", folder.getRoot().getPath());
        Assert.assertEquals("processed-data.json", written.get(0).getName());
        Assert.assertEquals("processed-entities-data.json", written.get(1).getName());
    }

    @Test
    public void nameCollisionsGetTheFileTypeInserted() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), "REDACTED_FILE", FileDataFormat.TXT),
                new FileOutput(b64("b"), "REDACTED_TEXT", FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals("processed-notes.txt", written.get(0).getName());
        Assert.assertEquals("processed-notes.redacted_text.txt", written.get(1).getName());
    }

    @Test
    public void createsMissingOutputDirectory() throws Exception {
        File nested = new File(folder.getRoot(), "a/b/c");
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "REDACTED_TEXT", FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", nested.getPath());
        Assert.assertTrue(nested.isDirectory());
        Assert.assertEquals(new File(nested, "processed-notes.txt"), written.get(0));
    }

    @Test
    public void presignedUrlOutputsAreNotWritten() throws Exception {
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput("https://s3/out", "REDACTED_FILE", FileDataFormat.PDF)),
                DataSourceType.PRESIGNED_URL, "invoice.pdf", folder.getRoot().getPath());
        Assert.assertTrue(written.isEmpty());
        Assert.assertEquals(0, folder.getRoot().list().length);
    }

    @Test
    public void emptyOrNullOutputsWriteNothing() throws Exception {
        Assert.assertTrue(FileOutputWriter.write(null, DataSourceType.BASE64, "x.txt", folder.getRoot().getPath()).isEmpty());
        Assert.assertTrue(FileOutputWriter.write(Collections.emptyList(), DataSourceType.BASE64, "x.txt", folder.getRoot().getPath()).isEmpty());
        Assert.assertTrue(FileOutputWriter.write(Collections.singletonList(new FileOutput(null, "ENTITIES", null)),
                DataSourceType.BASE64, "x.txt", folder.getRoot().getPath()).isEmpty());
    }

    @Test
    public void invalidBase64IsReportedAsWriteFailure() {
        try {
            FileOutputWriter.write(Collections.singletonList(new FileOutput("not base64!", "REDACTED_FILE", FileDataFormat.TXT)),
                    DataSourceType.BASE64, "x.txt", folder.getRoot().getPath());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("processed-x.txt"));
        }
    }

    @Test
    public void nameHelpers() {
        Assert.assertEquals("notes", FileOutputWriter.stripExtension("notes.txt"));
        Assert.assertEquals("archive.tar", FileOutputWriter.stripExtension("archive.tar.gz"));
        Assert.assertEquals("README", FileOutputWriter.stripExtension("README"));
        Assert.assertEquals(".hidden", FileOutputWriter.stripExtension(".hidden"));
        Assert.assertEquals("output", FileOutputWriter.stripExtension(null));
    }
}
