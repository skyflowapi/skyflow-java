package com.skyflow.detect.internal;

import com.skyflow.detect.DataSourceType;
import com.skyflow.detect.FileOutput;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Writes the base64 outputs of a successful file run next to, or into a chosen directory for, the
 * local file that was submitted. Internal to the SDK.
 *
 * <p>Each output becomes {@code processed-<input name>.<extension>}. Entity outputs carry their
 * type in the name so they never collide with a processed file of the same extension: ENTITIES
 * becomes {@code processed-entities-<input name>.json} and OBJECT_ENTITIES becomes
 * {@code processed-object-entities-<input name>.json}. If two outputs would still share a name
 * the file type is added before the extension. Presigned-URL outputs are not fetched and nothing
 * is written for them.
 */
public final class FileOutputWriter {
    private static final String ENTITIES_TYPE = "ENTITIES";

    private FileOutputWriter() {
    }

    /**
     * @return the files written, in output order; empty when the output type is not BASE64
     * @throws SkyflowException if the directory cannot be created or a file cannot be decoded or written
     */
    public static List<File> write(List<FileOutput> outputs, DataSourceType outputType, String inputFileName,
                                   String outputDirectory) throws SkyflowException {
        List<File> written = new ArrayList<>();
        if (outputs == null || outputs.isEmpty()) {
            return written;
        }
        if (outputType != DataSourceType.BASE64) {
            LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.OUTPUT_NOT_WRITTEN_FOR_OUTPUT_TYPE.getLog(),
                    String.valueOf(outputType)));
            return written;
        }
        File directory = outputDirectory == null || outputDirectory.trim().isEmpty() ? null : new File(outputDirectory);
        if (directory != null && !directory.isDirectory() && !directory.mkdirs()) {
            throw writeFailure(ErrorLogs.OUTPUT_DIRECTORY_NOT_CREATED, directory.getPath(), null);
        }
        String base = stripExtension(inputFileName);
        Set<String> used = new HashSet<>();
        for (FileOutput output : outputs) {
            if (output == null || output.getProcessedFile() == null) {
                continue;
            }
            String name = fileName(base, output, used);
            File target = directory == null ? new File(name) : new File(directory, name);
            try {
                byte[] bytes = Base64.getDecoder().decode(output.getProcessedFile());
                Files.write(target.toPath(), bytes);
            } catch (IllegalArgumentException | IOException e) {
                throw writeFailure(ErrorLogs.OUTPUT_FILE_NOT_WRITTEN, target.getPath(), e);
            }
            LogUtil.printInfoLog(BaseUtils.parameterizedString(InfoLogs.OUTPUT_FILE_WRITTEN.getLog(), target.getPath()));
            written.add(target);
        }
        return written;
    }

    /**
     * @param base the input file name without its extension
     * @return {@code processed-<base>.<ext>}, or {@code processed-<type>-<base>.json} for entity outputs
     */
    static String fileName(String base, FileOutput output, Set<String> used) {
        String type = output.getProcessedFileType() == null ? "" : output.getProcessedFileType();
        boolean entities = type.toUpperCase().contains(ENTITIES_TYPE);
        String extension;
        if (output.getProcessedFileExtension() != null) {
            extension = output.getProcessedFileExtension().name().toLowerCase();
        } else if (entities) {
            extension = "json";
        } else {
            extension = "bin";
        }
        // ENTITIES -> processed-entities-<base>, OBJECT_ENTITIES -> processed-object-entities-<base>
        String stem = entities
                ? BaseConstants.PROCESSED_FILE_NAME_PREFIX + type.toLowerCase().replace('_', '-') + "-" + base
                : BaseConstants.PROCESSED_FILE_NAME_PREFIX + base;
        String name = stem + "." + extension;
        if (!used.add(name)) {
            name = stem + "." + (type.isEmpty() ? String.valueOf(used.size()) : type.toLowerCase()) + "." + extension;
            used.add(name);
        }
        return name;
    }

    static String stripExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "output";
        }
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static SkyflowException writeFailure(ErrorLogs log, String path, Exception cause) {
        LogUtil.printErrorLog(BaseUtils.parameterizedString(log.getLog(), path));
        String message = BaseUtils.parameterizedString(BaseErrorMessage.FailedToWriteOutputFile.getMessage(), path);
        return cause == null ? new SkyflowException(message) : new SkyflowException(message, cause);
    }
}
