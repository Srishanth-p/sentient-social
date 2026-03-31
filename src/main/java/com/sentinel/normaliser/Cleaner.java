package com.sentinel.normaliser;

import com.sentinel.model.Post;
import com.sentinel.model.Profile;

import java.util.ArrayList;
import java.util.List;

// This class takes the raw data read by the ingestor
// and cleans it up so it is ready for the Risk Analysis Engine
// It makes sure all posts and profiles look the same
// regardless of which app they came from
public class Cleaner {

    // Cleans a list of posts and returns only the valid ones
    public List<Post> cleanPosts(List<Post> rawPosts) {

        List<Post> cleanedPosts = new ArrayList<>();

        for (Post post : rawPosts) {

            // Skip this post if it is completely empty or broken
            if (isBadPost(post)) {
                System.out.println("Skipping bad post: " + post);
                continue; // move on to the next post
            }

            // Clean each field of the post
            Post cleaned = new Post(
                cleanText(post.getPlatform()),
                cleanText(post.getContent()),
                cleanDate(post.getDate()),
                cleanText(post.getMediaType())
            );

            cleanedPosts.add(cleaned);
        }

        System.out.println("Posts cleaned: " + cleanedPosts.size()
                         + " kept out of " + rawPosts.size());
        return cleanedPosts;
    }

    // Cleans a profile and returns a safe version of it
    public Profile cleanProfile(Profile rawProfile) {

        if (rawProfile == null) {
            System.out.println("No profile found, returning empty profile.");
            return new Profile("unknown", "unknown", "unknown", "hidden", "unknown");
        }

        return new Profile(
            cleanText(rawProfile.getPlatform()),
            cleanText(rawProfile.getUsername()),
            cleanText(rawProfile.getFullName()),
            hideEmail(rawProfile.getEmail()),   // hide email for privacy
            cleanText(rawProfile.getBio())
        );
    }

    // ── HELPER METHODS ───────────────────────────────────────────────

   private boolean isBadPost(Post post) {

    // Only skip if the entire post object is null
    if (post == null) return true;

    // Check each field individually
    boolean noContent   = post.getContent() == null
                       || post.getContent().isBlank()
                       || post.getContent().equalsIgnoreCase("unknown");

    boolean noDate      = post.getDate() == null
                       || post.getDate().isBlank()
                       || post.getDate().equalsIgnoreCase("unknown");

    boolean noMediaType = post.getMediaType() == null
                       || post.getMediaType().isBlank()
                       || post.getMediaType().equalsIgnoreCase("unknown");

    // Only skip if ALL three fields are empty - truly nothing useful
    // An empty caption alone is fine - user just posted without text
    if (noContent && noDate && noMediaType) return true;

    return false;
}

    // Cleans up a text field
    // Removes extra spaces, handles null values safely
    private String cleanText(String text) {

        if (text == null)                    return "unknown";
        if (text.isBlank())                  return "no caption";
        if (text.equalsIgnoreCase("null"))   return "unknown";

        return text.trim(); // remove leading and trailing spaces
    }

    // Cleans and standardises a date field
    // For now we keep it as is but mark it clearly
    private String cleanDate(String date) {

        if (date == null || date.isBlank() || date.equalsIgnoreCase("unknown")) {
            return "date-unknown";
        }

        return date.trim();
    }

    // Hides the email address for privacy
    // e.g. "ravi@gmail.com" becomes "r***@gmail.com"
    private String hideEmail(String email) {

        if (email == null || email.isBlank() || email.equalsIgnoreCase("unknown")) {
            return "hidden";
        }

        // Split into username and domain e.g. ["ravi", "gmail.com"]
        String[] parts = email.split("@");

        if (parts.length != 2) {
            return "hidden"; // not a valid email format
        }

        // Keep only first letter, hide the rest with ***
        String hiddenUser = parts[0].charAt(0) + "***";
        return hiddenUser + "@" + parts[1];
    }
}