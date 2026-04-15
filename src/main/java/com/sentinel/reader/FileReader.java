package com.sentinel.reader;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileReader {

    private static final String TEMP_FOLDER = "temp_extracted";
    private static final int    CHUNK_SIZE  = 8192;
    private static final long   MAX_FILE_SIZE = 1024L * 1024L * 1024L;

    public String prepareFile(String filePath) {

        File file = new File(filePath);
        if (!file.exists()) {
            System.out.println("File not found: " + filePath);
            return filePath;
        }

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
            case "zip":  return handleZip(filePath);
            case "json": return filePath;
            case "html": return filePath;
            case "csv":  return filePath;
            default:
                System.out.println("Unknown type. Trying to read as JSON.");
                return filePath;
        }
    }

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

    // ── FIX: now preserves full subfolder structure inside the ZIP ──
    // Returns the root temp folder path, not a single file
    // This lets IngestorPicker and the ingestors navigate folders properly
    private String handleZip(String zipFilePath) {

        System.out.println("Extracting ZIP file (chunk by chunk)...");

        File tempDir = new File(TEMP_FOLDER);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        long totalExtracted = 0;

        try (ZipInputStream zipIn = new ZipInputStream(
                new BufferedInputStream(
                    new FileInputStream(zipFilePath), CHUNK_SIZE))) {

            ZipEntry entry;

            while ((entry = zipIn.getNextEntry()) != null) {

                String entryName = entry.getName();

                // Skip hidden/system files
                if (entryName.startsWith("__MACOSX") || entryName.startsWith(".")) {
                    zipIn.closeEntry();
                    continue;
                }

                // ── FIX: build the full output path preserving subfolders ──
                // e.g. "personal_information/instagram_profile_information.json"
                // becomes "temp_extracted/personal_information/instagram_profile_information.json"
                File outputFile = new File(TEMP_FOLDER + File.separator + entryName);

                if (entry.isDirectory()) {
                    // Create the folder so subfolders exist before files land in them
                    outputFile.mkdirs();
                    zipIn.closeEntry();
                    continue;
                }

                if (isUsefulFile(entryName)) {

                    // Make sure the parent folder exists
                    outputFile.getParentFile().mkdirs();

                    long fileSize    = extractFileInChunks(zipIn, outputFile.getPath());
                    totalExtracted  += fileSize;
                    long extractedMB = fileSize / (1024 * 1024);
                    System.out.println("Extracted: " + entryName
                        + " (" + extractedMB + " MB)");
                }

                zipIn.closeEntry();
            }

            long totalMB = totalExtracted / (1024 * 1024);
            System.out.println("Total extracted: " + totalMB + " MB");

        } catch (Exception e) {
            System.out.println("Error extracting ZIP: " + e.getMessage());
        }

        // ── FIX: return the root folder, not a single file ──
        System.out.println("Extraction complete. Root folder: " + TEMP_FOLDER);
        return TEMP_FOLDER;
    }

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
                long currentMB = totalBytes / (1024 * 1024);
                if (currentMB >= lastPrintedMB + 50) {
                    System.out.println("  Extracting... " + currentMB + " MB done");
                    lastPrintedMB = currentMB;
                }
            }
        }
        return totalBytes;
    }

    private boolean isUsefulFile(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".json") ||
               lower.endsWith(".html") ||
               lower.endsWith(".htm")  ||
               lower.endsWith(".csv");
    }

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