package com.example.detect;

import java.io.File;

import com.skyflow.Skyflow;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Env;
import com.skyflow.enums.LogLevel;
import com.skyflow.enums.MaskingMethod;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.FileInput;

/**
 * Skyflow Deidentify File Example
 * <p>
 * This example demonstrates how to use the Skyflow SDK to deidentify file
 * It has all available options for deidentifying files.
 * Supported file types: images (jpg, png, etc.), pdf, audio (mp3, wav), documents, spreadsheets, presentations, structured text.
 * It includes:
 * 1. Configure credentials
 * 2. Set up vault configuration
 * 3. Create a deidentify file request with all options
 * 4. Call deidentifyFile to deidentify file.
 * 5. Handle response and errors
 */
public class DeidentifyFileExample {

    public static void main(String[] args) throws SkyflowException {
        // Step 1: Set up credentials for the first vault configuration
        Credentials credentials = new Credentials();
        credentials.setPath("<YOUR_CREDENTIALS_FILE_PATH>"); // Replace with the path to the credentials file

        // Step 2: Configure the vault config
        VaultConfig vaultConfig = new VaultConfig();
        vaultConfig.setVaultId("<YOUR_VAULT_ID>");         // Replace with the ID of the vault
        vaultConfig.setClusterId("<YOUR_CLUSTER_ID>");     // Replace with the cluster ID of the vault
        vaultConfig.setEnv(Env.PROD);                       // Set the environment (e.g., DEV, STAGE, PROD)
        vaultConfig.setCredentials(credentials);           // Associate the credentials with the vault

        // Step 3: Create a Skyflow client
        Skyflow skyflowClient = Skyflow.builder()
                .setLogLevel(LogLevel.ERROR)               // Set log level to ERROR
                .addVaultConfig(vaultConfig)               // Add the vault configuration
                .build();

        try {

            // Step 4: Create a deidentify file request with all options

            // Create file object
            File file = new File("<FILE_PATH"); // Replace with the path to the file you want to deidentify

            // Create file input using the file object
            FileInput fileInput = FileInput.builder()
                    .file(file)
                    // .filePath("<FILE_PATH>") // Alternatively, you can use .filePath()
                    .build();

            // Output configuration
            String outputDirectory = "<OUTPUT_DIRECTORY>"; // Replace with the desired output directory to save the deidentified file

            // Entities to detect
            // List<DetectEntities> detectEntities = new ArrayList<>();
            // detectEntities.add(DetectEntities.IP_ADDRESS); // Replace with the entities you want to detect

            // Image-specific options
            // Boolean outputProcessedImage = true; // Include processed image in output
            // Boolean outputOcrText = true; // Include OCR text in output
            MaskingMethod maskingMethod = MaskingMethod.BLACKBOX; // Masking method for images

            // PDF-specific options
            // Integer pixelDensity = 15; //  Pixel density for PDF processing
            // Integer maxResolution = 2000; // Max resolution for PDF

            // Audio-specific options
            // Boolean outputProcessedAudio = true; // Include processed audio
            // DetectOutputTranscriptions outputTanscription = DetectOutputTranscriptions.PLAINTEXT_TRANSCRIPTION;  // Transcription type

            // Audio bleep configuration
            // AudioBleep audioBleep = AudioBleep.builder()
            //         .frequency(5D) // Pitch in Hz
            //         .startPadding(7D) // Padding at start (seconds)
            //         .stopPadding(8D) // Padding at end (seconds)
            //         .build();

            Integer waitTime = 20; // Max wait time for response (max 64 seconds)

            DeidentifyFileRequest deidentifyFileRequest = DeidentifyFileRequest.builder()
                    .file(fileInput)
                    .waitTime(waitTime)
                    // .entities(detectEntities)
                    .outputDirectory(outputDirectory)
                    .maskingMethod(maskingMethod)
                    // .outputProcessedImage(outputProcessedImage)
                    // .outputOcrText(outputOcrText)
                    // .pixelDensity(pixelDensity)
                    // .maxResolution(maxResolution)
                    // .outputProcessedAudio(outputProcessedAudio)
                    // .outputTranscription(outputTanscription)
                    // .bleep(audioBleep)
                    .build();


            DeidentifyFileResponse deidentifyFileResponse = skyflowClient.detect(vaultConfig.getVaultId()).deidentifyFile(deidentifyFileRequest);
            System.out.println("Deidentify file response: " + deidentifyFileResponse.toString());
        } catch (SkyflowException e) {
            System.err.println("Error occurred during deidentify file: ");
            e.printStackTrace();
        }
    }
}