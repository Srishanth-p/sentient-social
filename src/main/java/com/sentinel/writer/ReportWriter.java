package com.sentinel.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sentinel.model.OutputReport;

import java.io.File;

// This class takes the final clean report and does two things
// 1. Prints it nicely in the terminal
// 2. Saves it as a JSON file named after the user with a unique ID
public class ReportWriter {

    // Folder where all output reports will be saved
    private static final String OUTPUT_FOLDER = "output_reports";

    // Jackson mapper with pretty printing turned on
    // pretty printing means the JSON is nicely indented and easy to read
    private ObjectMapper mapper;

    public ReportWriter() {
        this.mapper = new ObjectMapper();
        // INDENT_OUTPUT makes the JSON file look neat with proper spacing
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    // Main method - takes the report and saves + prints it
    // returns the path of the saved file so we can tell the user where it is
    public String saveReport(OutputReport report) {

        // Create output folder if it does not exist
        File outputDir = new File(OUTPUT_FOLDER);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Build the file name from username and report ID
        // e.g. ravi_g_001.json
        String fileName = buildFileName(report);
        String filePath = OUTPUT_FOLDER + File.separator + fileName;

        try {
            // Save the report as a JSON file
            mapper.writeValue(new File(filePath), report);
            System.out.println("\nReport saved as: " + filePath);

        } catch (Exception e) {
            System.out.println("Error saving report: " + e.getMessage());
        }

        // Also print it in the terminal
        printReport(report);

        return filePath;
    }

    // Builds the file name using username and report ID
    // format: username_reportId.json  e.g. ravi_g_001.json
    private String buildFileName(OutputReport report) {

        String username = "unknown";

        // Try to get the username from the profile
        if (report.getProfile() != null &&
            report.getProfile().getUsername() != null &&
            !report.getProfile().getUsername().equalsIgnoreCase("unknown")) {

            // Clean the username so it is safe to use as a file name
            // replace spaces and special characters with underscore
            username = report.getProfile().getUsername()
                             .trim()
                             .replaceAll("[^a-zA-Z0-9_]", "_");
        }

        return username + "_" + report.getReportId() + ".json";
    }

    // Prints the report nicely in the terminal
    private void printReport(OutputReport report) {

        System.out.println("\n==============================================");
        System.out.println("   CLEAN OUTPUT REPORT");
        System.out.println("==============================================");
        System.out.println("Report ID    : " + report.getReportId());
        System.out.println("Platform     : " + report.getPlatform());
        System.out.println("Generated at : " + report.getGeneratedAt());
        System.out.println("----------------------------------------------");

        System.out.println("PROFILE:");
        if (report.getProfile() != null) {
            System.out.println("  Name     : " + report.getProfile().getFullName());
            System.out.println("  Username : " + report.getProfile().getUsername());
            System.out.println("  Email    : " + report.getProfile().getEmail());
            System.out.println("  Bio      : " + report.getProfile().getBio());
        }

        System.out.println("----------------------------------------------");
        System.out.println("POSTS (" + report.getPosts().size() + " clean / "
                                     + report.getTotalPosts() + " total / "
                                     + report.getSkippedPosts() + " skipped):");

        if (report.getPosts().isEmpty()) {
            System.out.println("  No valid posts found.");
        } else {
            for (int i = 0; i < report.getPosts().size(); i++) {
                System.out.println("  [" + (i + 1) + "] " + report.getPosts().get(i));
            }
        }

        System.out.println("==============================================");
    }
}