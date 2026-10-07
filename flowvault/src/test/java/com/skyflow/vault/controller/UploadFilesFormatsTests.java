package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.core.ApiClientHttpResponse;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.files.RawFilesClient;
import com.skyflow.generated.rest.resources.files.requests.FileUploadRequest;
import com.skyflow.generated.rest.types.FileUploadColumn;
import com.skyflow.generated.rest.types.FileUploadRecord;
import com.skyflow.generated.rest.types.FileUploadResponse;
import com.skyflow.generated.rest.types.FileUploadResponseObject;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.UploadFilesRequest;
import com.skyflow.vault.data.UploadFilesRequestColumn;
import com.skyflow.vault.data.UploadFilesRequestRecord;
import com.skyflow.vault.data.UploadFilesResponse;
import com.sun.net.httpserver.HttpServer;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * uploadFiles across file formats, end to end over real HTTP: Phase A is mocked to hand out signed
 * URLs that point at a local server, and the SDK's own uploader PUTs each file there. For every
 * format and every source (filePath, base64, fileObject) this checks the bytes that arrive, the
 * Content-Type sent, the file name announced to the vault, and that no Authorization header leaks.
 */
public class UploadFilesFormatsTests {

    /** One format under test: the file name to upload and the Content-Type it must be sent with. */
    private static final class Format {
        final String fileName;
        final String contentType;
        final byte[] magic;

        Format(String fileName, String contentType, int... magic) {
            this.fileName = fileName;
            this.contentType = contentType;
            this.magic = new byte[magic.length];
            for (int i = 0; i < magic.length; i++) {
                this.magic[i] = (byte) magic[i];
            }
        }

        String column() {
            return "col_" + fileName.replaceAll("[^A-Za-z0-9]", "_");
        }
    }

    // Each file starts with its format's real signature, so the content is representative of the
    // format and not just its extension.
    private static final List<Format> FORMATS = Arrays.asList(
            new Format("document.pdf", "application/pdf", 0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x37),
            new Format("photo.jpg", "image/jpeg", 0xFF, 0xD8, 0xFF, 0xE0),
            new Format("photo.jpeg", "image/jpeg", 0xFF, 0xD8, 0xFF, 0xE1),
            new Format("image.png", "image/png", 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A),
            new Format("anim.gif", "image/gif", 0x47, 0x49, 0x46, 0x38, 0x39, 0x61),
            new Format("bitmap.bmp", "image/bmp", 0x42, 0x4D),
            new Format("scan.tiff", "image/tiff", 0x49, 0x49, 0x2A, 0x00),
            new Format("picture.webp", "image/webp", 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50),
            new Format("iphone.heic", "image/heic", 0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63),
            new Format("logo.svg", "image/svg+xml", '<', 's', 'v', 'g'),
            new Format("notes.txt", "text/plain", 'h', 'e', 'l', 'l', 'o'),
            new Format("report.csv", "text/csv", 'a', ',', 'b', '\n'),
            new Format("payload.json", "application/json", '{', '"', 'k', '"', ':', '1', '}'),
            new Format("config.xml", "application/xml", '<', '?', 'x', 'm', 'l'),
            new Format("page.html", "text/html", '<', 'h', 't', 'm', 'l', '>'),
            new Format("letter.doc", "application/msword", 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1),
            new Format("letter.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    0x50, 0x4B, 0x03, 0x04),
            new Format("sheet.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    0x50, 0x4B, 0x03, 0x04),
            new Format("deck.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    0x50, 0x4B, 0x03, 0x04),
            new Format("archive.zip", "application/zip", 0x50, 0x4B, 0x03, 0x04),
            new Format("backup.tar.gz", "application/gzip", 0x1F, 0x8B, 0x08),
            new Format("voice.mp3", "audio/mpeg", 0x49, 0x44, 0x33, 0x03),
            new Format("call.wav", "audio/wav", 0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x41, 0x56, 0x45),
            new Format("clip.mp4", "video/mp4", 0, 0, 0, 0x20, 0x66, 0x74, 0x79, 0x70, 0x69, 0x73, 0x6F, 0x6D),
            new Format("movie.mov", "video/quicktime", 0, 0, 0, 0x14, 0x66, 0x74, 0x79, 0x70, 0x71, 0x74),
            // extension case, no extension, an unknown extension, and a name with spaces and accents
            new Format("PHOTO.JPG", "image/jpeg", 0xFF, 0xD8, 0xFF),
            new Format("Report.PDF", "application/pdf", 0x25, 0x50, 0x44, 0x46),
            new Format("README", "application/octet-stream", 'r', 'e', 'a', 'd'),
            new Format("blob.unknownext", "application/octet-stream", 0x00, 0x01, 0x02),
            new Format("résumé final 2026.pdf", "application/pdf", 0x25, 0x50, 0x44, 0x46));

    private enum Source { FILE_PATH, BASE64, FILE_OBJECT }

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private HttpServer server;
    private final Map<String, byte[]> receivedBodies = new ConcurrentHashMap<>();
    private final Map<String, String> receivedContentTypes = new ConcurrentHashMap<>();
    private final Map<String, String> receivedAuthorization = new ConcurrentHashMap<>();
    private final Map<String, String> receivedMethods = new ConcurrentHashMap<>();

    @Before
    public void startStorageServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            receivedMethods.put(path, exchange.getRequestMethod());
            receivedBodies.put(path, readAll(exchange.getRequestBody()));
            receivedContentTypes.put(path, String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type")));
            receivedAuthorization.put(path, String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            exchange.sendResponseHeaders(201, -1);
            exchange.close();
        });
        server.start();
    }

    @After
    public void stopStorageServer() {
        server.stop(0);
    }

    // ── fixtures ──────────────────────────────────────────────────────────────

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        while ((n = in.read(chunk)) > 0) {
            out.write(chunk, 0, n);
        }
        return out.toByteArray();
    }

    /** The format's signature, then every byte value 0-255, then seeded random bytes. */
    private static byte[] content(Format format, int randomBytes) {
        Random random = new Random(format.fileName.hashCode());
        byte[] tail = new byte[randomBytes];
        random.nextBytes(tail);
        byte[] all = new byte[format.magic.length + 256 + randomBytes];
        System.arraycopy(format.magic, 0, all, 0, format.magic.length);
        for (int i = 0; i < 256; i++) {
            all[format.magic.length + i] = (byte) i;
        }
        System.arraycopy(tail, 0, all, format.magic.length + 256, randomBytes);
        return all;
    }

    private File write(String subdirectory, String name, byte[] bytes) throws Exception {
        File dir = new File(folder.getRoot(), subdirectory);
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IllegalStateException("could not create " + dir);
        }
        File file = new File(dir, name);
        Files.write(file.toPath(), bytes);
        return file;
    }

    private UploadFilesRequestColumn column(Format format, Source source, byte[] bytes) throws Exception {
        UploadFilesRequestColumn.UploadFilesRequestColumnBuilder builder =
                UploadFilesRequestColumn.builder().column(format.column());
        switch (source) {
            case FILE_PATH:
                return builder.filePath(write("path", format.fileName, bytes).getPath()).build();
            case BASE64:
                return builder.base64(Base64.getEncoder().encodeToString(bytes)).fileName(format.fileName).build();
            default:
                return builder.fileObject(write("object", format.fileName, bytes)).build();
        }
    }

    // ── controller wiring ─────────────────────────────────────────────────────

    private static VaultController controller(ApiClient apiClient) throws Exception {
        return controller(apiClient, null);
    }

    private static VaultController controller(ApiClient apiClient, Integer vaultCallTimeoutSeconds) throws Exception {
        Credentials credentials = new Credentials();
        credentials.setApiKey("sky-ab123-abcd1234cdef1234abcd4321cdef4321");
        VaultConfig config = new VaultConfig();
        config.setVaultId("vault123");
        config.setClusterId("cluster123");
        config.setEnv(Env.DEV);
        config.setTimeout(vaultCallTimeoutSeconds);
        VaultController controller = new VaultController(config, credentials);
        Field field = VaultClient.class.getDeclaredField("apiClient");
        field.setAccessible(true);
        field.set(controller, apiClient);
        // Build the vault's real HTTP client, which stamps a bearer token on every request, so the
        // test proves the signed-URL upload does not inherit it.
        Method buildHttpClient = VaultClient.class.getDeclaredMethod("updateExecutorInHTTP");
        buildHttpClient.setAccessible(true);
        buildHttpClient.invoke(controller);
        return controller;
    }

    private static Response okResponse() {
        return new Response.Builder()
                .request(new Request.Builder().url("https://dummy.example.com").build())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .header(Constants.REQUEST_ID_HEADER_KEY, "req-files-1")
                .build();
    }

    private String signedUrl(int record, String column) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/upload/" + record + "/" + column
                + "?X-Goog-Signature=abc123";
    }

    /**
     * Mocks Phase A: answers every requested column of every record with a signed URL on the local
     * server, the way the vault does. Returns the raw files client so the request can be captured.
     */
    private RawFilesClient mockPhaseA(ApiClient apiClient) {
        FilesClient files = Mockito.mock(FilesClient.class);
        RawFilesClient raw = Mockito.mock(RawFilesClient.class);
        when(apiClient.files()).thenReturn(files);
        when(files.withRawResponse()).thenReturn(raw);
        when(raw.uploadFiles(any(), any())).thenAnswer(invocation -> {
            FileUploadRequest request = invocation.getArgument(0);
            List<FileUploadResponseObject> records = new ArrayList<>();
            for (int i = 0; i < request.getRecords().size(); i++) {
                FileUploadRecord record = request.getRecords().get(i);
                Map<String, Object> urls = new LinkedHashMap<>();
                for (FileUploadColumn column : record.getColumns()) {
                    urls.put(column.getColumn(), signedUrl(i, column.getColumn()));
                }
                records.add(FileUploadResponseObject.builder()
                        .skyflowId(record.getSkyflowId().orElse("sky-new-" + i))
                        .tableName(record.getTableName())
                        .httpCode(200)
                        .data(urls)
                        .build());
            }
            return new ApiClientHttpResponse<>(FileUploadResponse.builder().records(records).build(), okResponse());
        });
        return raw;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> columnsOf(Map<String, Object> record) {
        return (List<Map<String, Object>>) record.get("columns");
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    public void testEveryFormatFromEverySourceArrivesByteForByteWithItsContentType() throws Exception {
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        RawFilesClient raw = mockPhaseA(apiClient);
        VaultController controller = controller(apiClient);

        // one record per source, each carrying every format
        List<Source> sources = Arrays.asList(Source.values());
        Map<String, byte[]> expected = new HashMap<>();
        List<UploadFilesRequestRecord> records = new ArrayList<>();
        for (int r = 0; r < sources.size(); r++) {
            List<UploadFilesRequestColumn> columns = new ArrayList<>();
            for (Format format : FORMATS) {
                byte[] bytes = content(format, 4096);
                expected.put("/upload/" + r + "/" + format.column(), bytes);
                columns.add(column(format, sources.get(r), bytes));
            }
            records.add(UploadFilesRequestRecord.builder().tableName("documents").columns(columns).build());
        }

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(records).build());

        // Phase A announced each file under the name it will be stored as
        ArgumentCaptor<FileUploadRequest> captor = ArgumentCaptor.forClass(FileUploadRequest.class);
        Mockito.verify(raw).uploadFiles(captor.capture(), any());
        for (int r = 0; r < sources.size(); r++) {
            List<FileUploadColumn> announced = captor.getValue().getRecords().get(r).getColumns();
            for (int c = 0; c < FORMATS.size(); c++) {
                Assert.assertEquals(sources.get(r) + " " + FORMATS.get(c).fileName,
                        FORMATS.get(c).fileName, announced.get(c).getFileName().orElse(null));
            }
        }

        Assert.assertEquals(sources.size(), response.getRecords().size());
        for (int r = 0; r < sources.size(); r++) {
            Map<String, Object> record = response.getRecords().get(r);
            Assert.assertNull(record.get("error"));
            List<Map<String, Object>> columns = columnsOf(record);
            Assert.assertEquals(FORMATS.size(), columns.size());
            for (int c = 0; c < FORMATS.size(); c++) {
                Format format = FORMATS.get(c);
                String label = sources.get(r) + " " + format.fileName;
                String path = "/upload/" + r + "/" + format.column();
                Map<String, Object> column = columns.get(c);

                Assert.assertEquals(label, "UPLOADED", column.get("uploadStatus"));
                Assert.assertNull(label, column.get("error"));
                Assert.assertEquals(label, format.fileName, column.get("fileName"));
                Assert.assertEquals(label, "PUT", receivedMethods.get(path));
                Assert.assertArrayEquals(label, expected.get(path), receivedBodies.get(path));
                Assert.assertEquals(label, format.contentType, receivedContentTypes.get(path));
                Assert.assertEquals(label, "null", receivedAuthorization.get(path));
            }
        }
        Assert.assertEquals(sources.size() * FORMATS.size(), receivedBodies.size());
        // the signed URLs are credentials and must not reach the caller
        Assert.assertFalse(response.toString().contains("X-Goog-Signature"));
    }

    @Test
    public void testLargeBinaryFileIsStreamedIntact() throws Exception {
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        mockPhaseA(apiClient);
        VaultController controller = controller(apiClient);
        Format video = new Format("recording.mp4", "video/mp4", 0, 0, 0, 0x20, 0x66, 0x74, 0x79, 0x70);
        byte[] bytes = content(video, 12 * 1024 * 1024);

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("documents").columns(
                        Collections.singletonList(column(video, Source.FILE_PATH, bytes))).build())).build());

        Assert.assertEquals("UPLOADED", columnsOf(response.getRecords().get(0)).get(0).get("uploadStatus"));
        byte[] received = receivedBodies.get("/upload/0/" + video.column());
        Assert.assertEquals(bytes.length, received.length);
        Assert.assertArrayEquals(bytes, received);
    }

    @Test
    public void testEmptyFileIsSentAsAZeroLengthBody() throws Exception {
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        mockPhaseA(apiClient);
        VaultController controller = controller(apiClient);
        File empty = write("path", "empty.txt", new byte[0]);

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("documents").columns(
                        Collections.singletonList(UploadFilesRequestColumn.builder().column("notes")
                                .filePath(empty.getPath()).build())).build())).build());

        Assert.assertEquals("UPLOADED", columnsOf(response.getRecords().get(0)).get(0).get("uploadStatus"));
        Assert.assertEquals(0, receivedBodies.get("/upload/0/notes").length);
        Assert.assertEquals("text/plain", receivedContentTypes.get("/upload/0/notes"));
    }

    @Test
    public void testExplicitContentTypeAndFileNameOverrideWhatTheFileImplies() throws Exception {
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        RawFilesClient raw = mockPhaseA(apiClient);
        VaultController controller = controller(apiClient);
        byte[] pdf = "%PDF-1.7 scanned".getBytes(StandardCharsets.UTF_8);
        File scan = write("path", "scan.bin", pdf);

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("documents").columns(Arrays.asList(
                        // a misleading extension corrected by an explicit content type
                        UploadFilesRequestColumn.builder().column("scan").filePath(scan.getPath())
                                .contentType("application/pdf").build(),
                        // stored under a different name, still typed by that name
                        UploadFilesRequestColumn.builder().column("renamed").fileObject(scan)
                                .fileName("statement.pdf").build())).build())).build());

        List<Map<String, Object>> columns = columnsOf(response.getRecords().get(0));
        Assert.assertEquals("scan.bin", columns.get(0).get("fileName"));
        Assert.assertEquals("application/pdf", receivedContentTypes.get("/upload/0/scan"));
        Assert.assertEquals("statement.pdf", columns.get(1).get("fileName"));
        Assert.assertEquals("application/pdf", receivedContentTypes.get("/upload/0/renamed"));
        Assert.assertArrayEquals(pdf, receivedBodies.get("/upload/0/renamed"));

        ArgumentCaptor<FileUploadRequest> captor = ArgumentCaptor.forClass(FileUploadRequest.class);
        Mockito.verify(raw).uploadFiles(captor.capture(), any());
        Assert.assertEquals("statement.pdf",
                captor.getValue().getRecords().get(0).getColumns().get(1).getFileName().orElse(null));
    }

    @Test
    public void testStorageRejectingOneFormatFailsOnlyThatColumn() throws Exception {
        server.removeContext("/");
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            receivedBodies.put(path, readAll(exchange.getRequestBody()));
            String type = String.valueOf(exchange.getRequestHeaders().getFirst("Content-Type"));
            exchange.sendResponseHeaders(type.startsWith("video/") ? 415 : 200, -1);
            exchange.close();
        });
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        mockPhaseA(apiClient);
        VaultController controller = controller(apiClient);
        Format pdf = FORMATS.get(0);
        Format mp4 = new Format("clip.mp4", "video/mp4", 0, 0, 0, 0x20);
        Format png = new Format("image.png", "image/png", 0x89, 0x50, 0x4E, 0x47);

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("documents").columns(Arrays.asList(
                        column(pdf, Source.BASE64, content(pdf, 64)),
                        column(mp4, Source.BASE64, content(mp4, 64)),
                        column(png, Source.BASE64, content(png, 64)))).build())).build());

        List<Map<String, Object>> columns = columnsOf(response.getRecords().get(0));
        Assert.assertEquals("UPLOADED", columns.get(0).get("uploadStatus"));
        Assert.assertEquals("FAILED", columns.get(1).get("uploadStatus"));
        Assert.assertEquals("PUT failed: 415", columns.get(1).get("error"));
        Assert.assertEquals("UPLOADED", columns.get(2).get("uploadStatus"));
        Assert.assertNull(response.getRecords().get(0).get("error"));
        Assert.assertEquals(200, response.getRecords().get(0).get("httpCode"));
    }

    @Test
    public void testUploadLongerThanTheVaultCallTimeoutStillCompletes() throws Exception {
        // storage takes ~2s to accept the file, twice the 1s ceiling configured for vault calls
        server.removeContext("/");
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            receivedBodies.put(path, readAll(exchange.getRequestBody()));
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        ApiClient apiClient = Mockito.mock(ApiClient.class);
        mockPhaseA(apiClient);
        VaultController controller = controller(apiClient, 1);
        Format pdf = FORMATS.get(0);
        byte[] bytes = content(pdf, 1024);

        UploadFilesResponse response = controller.uploadFiles(UploadFilesRequest.builder().records(
                Collections.singletonList(UploadFilesRequestRecord.builder().tableName("documents").columns(
                        Collections.singletonList(column(pdf, Source.FILE_PATH, bytes))).build())).build());

        Map<String, Object> column = columnsOf(response.getRecords().get(0)).get(0);
        Assert.assertEquals(String.valueOf(column.get("error")), "UPLOADED", column.get("uploadStatus"));
        Assert.assertArrayEquals(bytes, receivedBodies.get("/upload/0/" + pdf.column()));
    }
}
