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

    // ---- naming branches -------------------------------------------------------------------

    @Test
    public void nullOrEmptyTypeFallsBackToPlainName() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), null, FileDataFormat.PDF),
                new FileOutput(b64("b"), "", FileDataFormat.TXT)),
                DataSourceType.BASE64, "invoice.pdf", folder.getRoot().getPath());
        Assert.assertEquals("processed-invoice.pdf", written.get(0).getName());
        Assert.assertEquals("processed-invoice.txt", written.get(1).getName());
    }

    @Test
    public void nonEntityOutputWithoutExtensionGetsBin() throws Exception {
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "REDACTED_TRANSCRIPTION", null)),
                DataSourceType.BASE64, "call.mp3", folder.getRoot().getPath());
        Assert.assertEquals("processed-call.bin", written.get(0).getName());
    }

    @Test
    public void entityOutputHonoursAnExplicitExtension() throws Exception {
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "ENTITIES", FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals("processed-entities-notes.txt", written.get(0).getName());
    }

    @Test
    public void entityTypeMatchIsCaseInsensitive() throws Exception {
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "entities", null)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals("processed-entities-notes.json", written.get(0).getName());
    }

    @Test
    public void duplicateEntityOutputsGetTheTypeInsertedOnCollision() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), "ENTITIES", FileDataFormat.JSON),
                new FileOutput(b64("b"), "ENTITIES", FileDataFormat.JSON)),
                DataSourceType.BASE64, "scan.png", folder.getRoot().getPath());
        Assert.assertEquals("processed-entities-scan.json", written.get(0).getName());
        Assert.assertEquals("processed-entities-scan.entities.json", written.get(1).getName());
    }

    @Test
    public void collisionWithoutATypeUsesARunningNumber() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), null, FileDataFormat.TXT),
                new FileOutput(b64("b"), null, FileDataFormat.TXT),
                new FileOutput(b64("c"), null, FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals("processed-notes.txt", written.get(0).getName());
        Assert.assertEquals("processed-notes.1.txt", written.get(1).getName());
        Assert.assertEquals("processed-notes.2.txt", written.get(2).getName());
    }

    @Test
    public void inputNamesWithoutAnExtensionOrWithLeadingDotAreKept() throws Exception {
        List<File> plain = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "ENTITIES", null)),
                DataSourceType.BASE64, "README", folder.getRoot().getPath());
        Assert.assertEquals("processed-entities-README.json", plain.get(0).getName());

        List<File> dotted = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "REDACTED_TEXT", FileDataFormat.TXT)),
                DataSourceType.BASE64, ".notes", folder.getRoot().getPath());
        Assert.assertEquals("processed-.notes.txt", dotted.get(0).getName());
    }

    @Test
    public void missingInputNameFallsBackToOutput() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                new FileOutput(b64("a"), "REDACTED_FILE", FileDataFormat.PDF),
                new FileOutput(b64("b"), "ENTITIES", null)),
                DataSourceType.BASE64, null, folder.getRoot().getPath());
        Assert.assertEquals("processed-output.pdf", written.get(0).getName());
        Assert.assertEquals("processed-entities-output.json", written.get(1).getName());
    }

    // ---- directory and output-type branches ---------------------------------------------------

    @Test
    public void nullOutputTypeWritesNothing() throws Exception {
        List<File> written = FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("x"), "REDACTED_FILE", FileDataFormat.PDF)),
                null, "invoice.pdf", folder.getRoot().getPath());
        Assert.assertTrue(written.isEmpty());
        Assert.assertEquals(0, folder.getRoot().list().length);
    }

    @Test
    public void nullElementsInTheOutputListAreSkipped() throws Exception {
        List<File> written = FileOutputWriter.write(Arrays.asList(
                null,
                new FileOutput(b64("x"), "REDACTED_TEXT", FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals(1, written.size());
        Assert.assertEquals("processed-notes.txt", written.get(0).getName());
    }

    @Test
    public void blankOutputDirectoryWritesToTheWorkingDirectory() throws Exception {
        File expected = new File("processed-writer-cwd-test.txt");
        try {
            List<File> written = FileOutputWriter.write(
                    Collections.singletonList(new FileOutput(b64("x"), "REDACTED_TEXT", FileDataFormat.TXT)),
                    DataSourceType.BASE64, "writer-cwd-test.txt", "   ");
            Assert.assertEquals(expected, written.get(0));
            Assert.assertNull(written.get(0).getParentFile());
            Assert.assertTrue(expected.isFile());
        } finally {
            Files.deleteIfExists(expected.toPath());
        }
    }

    @Test
    public void outputDirectoryThatIsAFileIsReportedAsWriteFailure() throws Exception {
        File notADirectory = folder.newFile("blocker");
        try {
            FileOutputWriter.write(
                    Collections.singletonList(new FileOutput(b64("x"), "REDACTED_TEXT", FileDataFormat.TXT)),
                    DataSourceType.BASE64, "notes.txt", notADirectory.getPath());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains(notADirectory.getPath()));
        }
    }

    @Test
    public void existingFilesAreOverwritten() throws Exception {
        File target = new File(folder.getRoot(), "processed-notes.txt");
        Files.write(target.toPath(), "old".getBytes());
        FileOutputWriter.write(
                Collections.singletonList(new FileOutput(b64("new"), "REDACTED_TEXT", FileDataFormat.TXT)),
                DataSourceType.BASE64, "notes.txt", folder.getRoot().getPath());
        Assert.assertEquals("new", new String(Files.readAllBytes(target.toPath())));
    }

    @Test
    public void aLaterFailureLeavesEarlierFilesOnDisk() throws Exception {
        try {
            FileOutputWriter.write(Arrays.asList(
                    new FileOutput(b64("ok"), "REDACTED_FILE", FileDataFormat.PDF),
                    new FileOutput("not base64!", "ENTITIES", null)),
                    DataSourceType.BASE64, "invoice.pdf", folder.getRoot().getPath());
            Assert.fail("expected SkyflowException");
        } catch (SkyflowException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("processed-entities-invoice.json"));
        }
        Assert.assertTrue(new File(folder.getRoot(), "processed-invoice.pdf").isFile());
        Assert.assertFalse(new File(folder.getRoot(), "processed-entities-invoice.json").exists());
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
