package com.sentinel.reader;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

// This class prepares the user's uploaded file for reading
// It handles ZIP extraction in small chunks
// so even a 1GB ZIP file won't crash the program
public class FileReader {

    private static final String TEMP_FOLDER = "temp_extracted";

    // Chunk size for reading ZIP contents - 8KB at a time
    // This means we never hold more than 8KB in memory while extracting
    private static final int CHUNK_SIZE = 8192;

    // Max file size we allow - 1GB in bytes
    private static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L;

    // Main method - prepares the file and returns path ready to read
    public String prepareFile(String filePath) {

        // Check if file exists
        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return filePath;
        }

        // Check file size before doing anything
        long fileSizeBytes = file.length();
        long fileSizeMB    = fileSizeBytes / (1024 * 1024);
        System.out.println("File size: " + fileSizeMB + " MB");

        if (fileSizeBytes > MAX_FILE_SIZE) {
            System.out.println("Warning: file is larger than 1GB ("
                + fileSizeMB + " MB). May be slow.");
        }

        String fileType = detectFileType(filePath);
        System.out.println("File type detected: " + fileType.toUpperCase());

        switch (fileType) {
            case "zip":
                return handleZip(filePath);
            case "json":
                return filePath;
            case "html":
                return filePath;
            case "csv":
                return filePath;
            default:
                System.out.println("Unknown type. Trying to read as JSON.");
                return filePath;
        }
    }

    // Detects file type from extension
    public String detectFileType(String filePath) {

        if (filePath == null || filePath.isBlank()) return "unknown";

        String lower = filePath.toLowerCase().trim();

        if (lower.endsWith(".zip"))  return "zip";
        if (lower.endsWith(".json")) return "json";
        if (lower.endsWith(".html")) return "html";
        if (lower.endsWith(".htm"))  return "html";
        if (lower.endsWith(".csv"))  return "csv";

        return "unknown";
    }

    // Extracts ZIP file in small chunks - memory safe for large ZIPs
    private String handleZip(String zipFilePath) {

        System.out.println("Extracting ZIP file (chunk by chunk)...");

        File tempDir = new File(TEMP_FOLDER);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String firstUsefulFile = null;
        long   totalExtracted  = 0;

        try (ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(
                    new FileInputStream(zipFilePath), CHUNK_SIZE))) {

            ZipEntry entry;

            while ((entry = zipIn.getNextEntry()) != null) {

                String entryName = entry.getName();

                // Skip folders and hidden system files
                if (entry.isDirectory()
                        || entryName.startsWith("__MACOSX")
                        || entryName.startsWith(".")) {
                    zipIn.closeEntry();
                    continue;
                }

                if (isUsefulFile(entryName)) {

                    String fileName   = new File(entryName).getName();
                    String outputPath = TEMP_FOLDER + File.separator + fileName;

                    // Extract in chunks - never loads whole file into memory
                    long fileSize = extractFileInChunks(zipIn, outputPath);
                    totalExtracted += fileSize;

                    long extractedMB = fileSize / (1024 * 1024);
                    System.out.println("Extracted: " + fileName
                        + " (" + extractedMB + " MB)");

                    if (firstUsefulFile == null) {
                        firstUsefulFile = outputPath;
                    }
                }

                zipIn.closeEntry();
            }

            long totalMB = totalExtracted / (1024 * 1024);
            System.out.println("Total extracted: " + totalMB + " MB");

        } catch (Exception e) {
            System.out.println("Error extracting ZIP: " + e.getMessage());
        }

        if (firstUsefulFile != null) {
            System.out.println("Using file: " + firstUsefulFile);
            return firstUsefulFile;
        }

        return zipFilePath;
    }

    // Extracts one file from ZIP in 8KB chunks
    // returns the size of the extracted file in bytes
    private long extractFileInChunks(ZipInputStream zipIn,
                                     String outputPath) throws IOException {

        byte[] buffer        = new byte[CHUNK_SIZE];
        long   totalBytes    = 0;
        long   lastPrintedMB = 0;

        try (BufferedOutputStream bos = new BufferedOutputStream(
                new FileOutputStream(outputPath), CHUNK_SIZE)) {

            int bytesRead;

            while ((bytesRead = zipIn.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                // Show progress every 50MB extracted
                long currentMB = totalBytes / (1024 * 1024);
                if (currentMB >= lastPrintedMB + 50) {
                    System.out.println("  Extracting... " + currentMB + " MB done");
                    lastPrintedMB = currentMB;
                }
            }
        }

        return totalBytes;
    }

    // Checks if a file inside ZIP is worth extracting
    private boolean isUsefulFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".json") ||
               lower.endsWith(".html") ||
               lower.endsWith(".htm")  ||
               lower.endsWith(".csv");
    }

    // Cleans up temp folder after we are done
    public void cleanUp() {
        File tempDir = new File(TEMP_FOLDER);
        if (tempDir.exists()) {
            deleteFolder(tempDir);
            System.out.println("Temp files cleaned up.");
        }
    }

    private void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) deleteFolder(f);
                else f.delete();
            }
        }
        folder.delete();
    }
}