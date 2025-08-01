package com.skyflow.vault.detect;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileInfoTest {

    @Test
    public void testFileInfoFields() throws IOException {
        // Create a temp file
        File tempFile = File.createTempFile("testfileinfo", ".txt");
        tempFile.deleteOnExit();

        // Write some content to ensure size > 0
        FileWriter writer = new FileWriter(tempFile);
        writer.write("Hello Skyflow!");
        writer.close();

        FileInfo fileInfo = new FileInfo(tempFile);

        Assert.assertEquals(tempFile.getName(), fileInfo.getName());
        Assert.assertEquals(tempFile.length(), fileInfo.getSize());
        Assert.assertEquals("", fileInfo.getType());
        Assert.assertEquals(tempFile.lastModified(), fileInfo.getLastModified());
    }

    @Test
    public void testFileInfoWithNonExistentFile() {
        File fakeFile = new File("nonexistentfile.txt");
        FileInfo fileInfo = new FileInfo(fakeFile);

        Assert.assertEquals("nonexistentfile.txt", fileInfo.getName());
        Assert.assertEquals(0, fileInfo.getSize());
        Assert.assertEquals("", fileInfo.getType());
        Assert.assertEquals(0, fileInfo.getLastModified());
    }
}