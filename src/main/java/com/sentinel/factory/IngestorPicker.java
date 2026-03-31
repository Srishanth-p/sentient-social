package com.sentinel.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.ingestor.GenericIngestor;
import com.sentinel.ingestor.Ingestor;

import java.io.File;

// This class does two things:
// 1. Detects which social media app the file came from automatically
// 2. Returns the right ingestor for that app
// User just gives us the file - we figure out the app ourselves
public class IngestorPicker {

    // Tries to detect the app name by looking inside the file
    // Returns app name e.g. "instagram", "linkedin", "facebook"
    // or "unknown" if we cannot figure it out
    public static String detectAppName(String filePath) {

        if (filePath == null || filePath.isBlank()) return "unknown";

        String lower = filePath.toLowerCase();

        // First try: check the file name itself for clues
        // Many apps name their export files with the app name
        if (lower.contains("instagram")) return "instagram";
        if (lower.contains("linkedin"))  return "linkedin";
        if (lower.contains("facebook"))  return "facebook";
        if (lower.contains("twitter"))   return "twitter";
        if (lower.contains("reddit"))    return "reddit";
        if (lower.contains("snapchat"))  return "snapchat";

        // Second try: look inside the JSON file for clues
        if (lower.endsWith(".json")) {
            String detectedFromContent = detectFromJsonContent(filePath);
            if (!detectedFromContent.equals("unknown")) {
                return detectedFromContent;
            }
        }

        // Could not detect - use generic
        System.out.println("Could not detect app name. Using generic ingestor.");
        return "unknown";
    }

    // Looks inside the JSON file for fields that hint at which app it is from
    private static String detectFromJsonContent(String filePath) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root       = mapper.readTree(new File(filePath));

            // Instagram exports usually have these fields
            if (root.has("media") || root.has("stories") ||
                root.has("profile_user") || root.has("ig_username")) {
                return "instagram";
            }

            // LinkedIn exports usually have these fields
            if (root.has("connections") || root.has("positions") ||
                root.has("endorsements") || root.has("languages")) {
                return "linkedin";
            }

            // Facebook exports usually have these fields
            if (root.has("profile_v2") || root.has("friends") ||
                root.has("timeline")) {
                return "facebook";
            }

            // Twitter exports usually have these fields
            if (root.has("tweets") || root.has("like") ||
                root.has("following")) {
                return "twitter";
            }

        } catch (Exception e) {
            System.out.println("Could not read file to detect app: " + e.getMessage());
        }

        return "unknown";
    }

    // Returns the right ingestor based on app name
    // Right now everything goes to GenericIngestor
    // Later you can plug in specific ones here
    public static Ingestor getIngestor(String appName) {

        if (appName == null || appName.isBlank()) {
            return new GenericIngestor("unknown");
        }

        String name = appName.toLowerCase().trim();

        // When you want to add specific ingestors later:
        // if (name.equals("instagram")) return new InstagramIngestor();
        // if (name.equals("linkedin"))  return new LinkedinIngestor();

        System.out.println("Loading ingestor for: " + name);
        return new GenericIngestor(name);
    }
}