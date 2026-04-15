package com.sentinel.ingestor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.model.Post;
import com.sentinel.model.Profile;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class InstagramIngestor implements Ingestor {

    private final ObjectMapper      mapper = new ObjectMapper();
    private final DateTimeFormatter fmt    = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Override
    public String getPlatformName() {
        return "instagram";
    }

    @Override
    public Profile readProfile(String folderPath) {
        try {
            File f = new File(folderPath,
                "personal_information/personal_information/personal_information.json");

            JsonNode root = mapper.readTree(f);
            JsonNode data = root.path("profile_user")
                               .get(0)
                               .path("string_map_data");

            String username = data.path("Username").path("value").asText("unknown");
            String fullName = data.path("Name").path("value").asText("unknown");
            String email    = data.path("Email address").path("value").asText("");
            String dob      = data.path("Date of birth").path("value").asText("");
            String bio      = dob.isEmpty() ? "" : "DOB: " + dob;

            System.out.println("Profile loaded: " + fullName + " (@" + username + ")");
            return new Profile("instagram", username, fullName, email, bio);

        } catch (Exception e) {
            System.out.println("Could not read Instagram profile: " + e.getMessage());
            return new Profile("instagram", "unknown", "unknown", "", "");
        }
    }

    @Override
    public List<Post> readPosts(String folderPath) {

        List<Post> posts = new ArrayList<>();

        // 1. Comments on other people's posts
        readComments(
            new File(folderPath,
                "your_instagram_activity/comments/post_comments_1.json"),
            null,
            "post_comment",
            posts
        );

        // 2. Your own feed posts with captions
        readOwnPosts(
            new File(folderPath,
                "your_instagram_activity/media/posts_1.json"),
            posts
        );

        // 3. Your stories with captions
        readStories(
            new File(folderPath,
                "your_instagram_activity/media/stories.json"),
            posts
        );

        System.out.println("Total Instagram posts loaded: " + posts.size());
        return posts;
    }

    // Handles both plain array and wrapped object comment files
    private void readComments(File file, String wrapperKey,
                              String mediaType, List<Post> posts) {
        if (!file.exists()) {
            System.out.println("Skipping (not found): " + file.getName());
            return;
        }
        int before = posts.size();
        try {
            JsonNode root  = mapper.readTree(file);
            JsonNode array = (wrapperKey != null) ? root.path(wrapperKey) : root;

            for (JsonNode item : array) {
                JsonNode smd     = item.path("string_map_data");
                String   content = fixEncoding(
                    smd.path("Comment").path("value").asText("").trim()
                );
                long   ts   = smd.path("Time").path("timestamp").asLong(0);
                String date = ts > 0
                        ? fmt.format(Instant.ofEpochSecond(ts))
                        : "unknown";

                if (!content.isEmpty()) {
                    posts.add(new Post("instagram", content, date, mediaType));
                }
            }
            System.out.println("Read " + (posts.size() - before)
                + " entries from " + file.getName());

        } catch (Exception e) {
            System.out.println("Error reading " + file.getName()
                + ": " + e.getMessage());
        }
    }

    // Reads your own feed posts - caption is inside media[0].title
    private void readOwnPosts(File file, List<Post> posts) {
        if (!file.exists()) {
            System.out.println("Skipping (not found): " + file.getName());
            return;
        }
        int before = posts.size();
        try {
            JsonNode root  = mapper.readTree(file);
            JsonNode array = root.isArray() ? root : root.path("media");

            for (JsonNode item : array) {
                JsonNode mediaArray = item.path("media");
                if (!mediaArray.isArray() || mediaArray.size() == 0) continue;

                JsonNode first   = mediaArray.get(0);
                String   content = fixEncoding(
                    first.path("title").asText("").trim()
                );
                long   ts   = first.path("creation_timestamp").asLong(0);
                String date = ts > 0
                        ? fmt.format(Instant.ofEpochSecond(ts))
                        : "unknown";

                if (!content.isEmpty()) {
                    posts.add(new Post("instagram", content, date, "post"));
                }
            }
            System.out.println("Read " + (posts.size() - before)
                + " own posts from " + file.getName());

        } catch (Exception e) {
            System.out.println("Error reading " + file.getName()
                + ": " + e.getMessage());
        }
    }

    // Reads stories - caption is directly in the title field of each item
    private void readStories(File file, List<Post> posts) {
        if (!file.exists()) {
            System.out.println("Skipping (not found): " + file.getName());
            return;
        }
        int before = posts.size();
        try {
            JsonNode root  = mapper.readTree(file);
            JsonNode array = root.path("ig_stories");

            for (JsonNode item : array) {
                String content = fixEncoding(
                    item.path("title").asText("").trim()
                );
                long   ts   = item.path("creation_timestamp").asLong(0);
                String date = ts > 0
                        ? fmt.format(Instant.ofEpochSecond(ts))
                        : "unknown";

                if (!content.isEmpty()) {
                    posts.add(new Post("instagram", content, date, "story"));
                }
            }
            System.out.println("Read " + (posts.size() - before)
                + " stories from " + file.getName());

        } catch (Exception e) {
            System.out.println("Error reading " + file.getName()
                + ": " + e.getMessage());
        }
    }

    // Fixes mojibake encoding - Instagram sometimes double-encodes UTF-8 as Latin-1
    private String fixEncoding(String text) {
        try {
            byte[] bytes = text.getBytes("ISO-8859-1");
            String fixed = new String(bytes, "UTF-8");
            return fixed.contains("\uFFFD") ? text : fixed;
        } catch (Exception e) {
            return text;
        }
    }
}