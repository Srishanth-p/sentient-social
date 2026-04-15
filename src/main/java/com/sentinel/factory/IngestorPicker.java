package com.sentinel.factory;

import com.sentinel.ingestor.GenericIngestor;
import com.sentinel.ingestor.Ingestor;
import com.sentinel.ingestor.InstagramIngestor;

import java.io.File;

public class IngestorPicker {

    // Detects which platform the export came from
    // by checking for known files/folders in the extracted directory
    public static String detectAppName(String folderPath) {

        // Try detecting directly first
        String result = detectInFolder(folderPath);
        if (!result.equals("unknown")) return result;

        // If not found, check one level deeper (handles ZIPs with a root subfolder)
        File root = new File(folderPath);
        File[] subFolders = root.listFiles(File::isDirectory);
        if (subFolders != null) {
            for (File sub : subFolders) {
                result = detectInFolder(sub.getAbsolutePath());
                if (!result.equals("unknown")) {
                    System.out.println("Detected inside subfolder: " + sub.getName());
                    return result + "|" + sub.getAbsolutePath();
                }
            }
        }

        return "unknown";
    }

    private static String detectInFolder(String path) {
        File igProfile = new File(path,
            "personal_information/personal_information/personal_information.json");
        if (igProfile.exists()) return "instagram";

        File linkedIn = new File(path, "Profile.csv");
        if (linkedIn.exists()) return "linkedin";

        return "unknown";
    }

    // Returns the correct ingestor for the detected platform
    public static Ingestor getIngestor(String appName) {
        // appName may contain "|realPath" suffix from subfolder detection
        String name = appName.contains("|") ? appName.split("\\|")[0] : appName;
        switch (name.toLowerCase()) {
            case "instagram": return new InstagramIngestor();
            default:
                System.out.println("Loading ingestor for: unknown");
                return new GenericIngestor("unknown");
        }
    }
}