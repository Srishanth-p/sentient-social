package com.sentinel;

import com.sentinel.factory.IngestorPicker;
import com.sentinel.ingestor.Ingestor;
import com.sentinel.model.OutputReport;
import com.sentinel.model.Post;
import com.sentinel.model.Profile;
import com.sentinel.normaliser.Cleaner;
import com.sentinel.reader.FileReader;
import com.sentinel.risk.engine.RiskEngine;
import com.sentinel.writer.ReportWriter;

import javax.swing.JFileChooser;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

// This is the main entry point of the program
// It opens a file picker window so the user can select their export file
// No typing paths - just click and select like any normal app
public class Main {

    // Gives each report a unique number e.g. 001, 002, 003
    private static AtomicInteger reportCounter = new AtomicInteger(1);

    public static void main(String[] args) {

        System.out.println("==============================================");
        System.out.println("   Sentinel Social - Ingestion Pipeline");
        System.out.println("==============================================");
        System.out.println("Opening file picker...");

        // ── OPEN FILE PICKER WINDOW ───────────────────────────────────
        String filePath = openFilePicker();

        // If user closed the window without picking a file
        if (filePath == null) {
            System.out.println("No file selected. Exiting.");
            return;
        }

        System.out.println("File selected : " + filePath);
        System.out.println("----------------------------------------------");

        // Run the full pipeline with the file the user picked
        runPipeline(filePath);
    }

    // Opens a file picker window and returns the path of the selected file
    // Returns null if user cancels or closes the window
    private static String openFilePicker() {

        try {
            // Make the file picker look like a normal Windows window
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // If styling fails it still works just looks more basic
        }

        // Create the file picker
        JFileChooser picker = new JFileChooser();
        picker.setDialogTitle("Select your social media data export file");

        // Only show supported file types in the picker
        // User will not see .exe or .txt files - only what we support
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Supported files (ZIP, JSON, HTML, CSV)",
            "zip", "json", "html", "htm", "csv"
        );
        picker.setFileFilter(filter);

        // Start the picker in the user's Downloads folder
        // because most exported files end up there
        String downloadsPath = System.getProperty("user.home") + File.separator + "Downloads";
        File downloadsFolder = new File(downloadsPath);
        if (downloadsFolder.exists()) {
            picker.setCurrentDirectory(downloadsFolder);
        }

        // Show the window and wait for user to pick a file
        int result = picker.showOpenDialog(null);

        // User picked a file
        if (result == JFileChooser.APPROVE_OPTION) {
            return picker.getSelectedFile().getAbsolutePath();
        }

        // User closed or cancelled
        return null;
    }

    // Full pipeline - takes a file path and produces a clean JSON report
    // This is the method Component 4 (UI) will call directly
    public static String runPipeline(String filePath) {

        // ── STEP 1: Prepare the file ──────────────────────────────────
        System.out.println("\n[1] Preparing file...");
        FileReader fileReader   = new FileReader();
        String preparedFilePath = fileReader.prepareFile(filePath);

        // ── STEP 2: Detect which app and resolve real root path ──────────────
        System.out.println("\n[2] Detecting app...");
        String detectedRaw = IngestorPicker.detectAppName(preparedFilePath);

        // If subfolder was found, split out the real path
        String appName;
        String realRootPath;
        if (detectedRaw.contains("|")) {
            String[] parts = detectedRaw.split("\\|", 2);
            appName      = parts[0];
            realRootPath = parts[1];
            System.out.println("App detected: " + appName);
            System.out.println("Real root   : " + realRootPath);
        } else {
            appName      = detectedRaw;
            realRootPath = preparedFilePath;
            System.out.println("App detected: " + appName);
        }

        // ── STEP 3: Pick the right ingestor ──────────────────────────────────
        System.out.println("\n[3] Loading ingestor...");
        Ingestor ingestor = IngestorPicker.getIngestor(appName);

        // ── STEP 4: Read raw data using the resolved root path ────────────────
        System.out.println("\n[4] Reading data from file...");
        List<Post> rawPosts   = ingestor.readPosts(realRootPath);
        Profile    rawProfile = ingestor.readProfile(realRootPath);

        System.out.println("Raw posts found   : " + rawPosts.size());
        System.out.println("Raw profile found : " + (rawProfile != null ? "yes" : "no"));

        // ── STEP 5: Clean the data ────────────────────────────────────
        System.out.println("\n[5] Cleaning data...");
        Cleaner    cleaner      = new Cleaner();
        List<Post> cleanPosts   = cleaner.cleanPosts(rawPosts);
        Profile    cleanProfile = cleaner.cleanProfile(rawProfile);

        int skipped = rawPosts.size() - cleanPosts.size();

        // ── STEP 6: Build the output report ───────────────────────────
        System.out.println("\n[6] Building report...");

        String reportId    = String.format("%03d", reportCounter.getAndIncrement());
        String generatedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        OutputReport report = new OutputReport(
            reportId,
            appName,
            generatedAt,
            cleanProfile,
            cleanPosts,
            rawPosts.size(),
            skipped
        );

        // ── STEP 7: Save and print ────────────────────────────────────
        System.out.println("\n[7] Saving report...");
        ReportWriter writer    = new ReportWriter();
        String       savedPath = writer.saveReport(report);

        // ── STEP 8: Run Risk Analysis Engine ─────────────────────────────────
        System.out.println("\n[8] Running risk analysis...");
        RiskEngine riskEngine  = new RiskEngine();
        OutputReport enriched  = riskEngine.analyse(report);

        // Re-save the enriched report with risk scores attached
        String riskReportPath  = writer.saveReport(enriched);
        System.out.println("Risk report saved: " + riskReportPath);

        // ── STEP 9: Clean up temp files if ZIP was used ───────────────
        fileReader.cleanUp();

        System.out.println("\nPipeline complete!");
        System.out.println("Output JSON ready for Risk Analysis Engine: " + savedPath);
        System.out.println("==============================================");

        return savedPath;
    }
}