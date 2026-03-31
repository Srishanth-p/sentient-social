package com.sentinel.ingestor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinel.model.Post;
import com.sentinel.model.Profile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

// This class reads a JSON export file from ANY social media app
// It uses STREAMING mode - meaning it reads the file piece by piece
// instead of loading the whole thing into memory at once
// This allows us to handle large files (500MB+) without crashing
public class GenericIngestor implements Ingestor {

    private String platformName;
    private ObjectMapper mapper;

    // How many posts to process at a time
    // This keeps memory usage low even for huge files
    private static final int BATCH_SIZE = 100;

    public GenericIngestor(String platformName) {
        this.platformName = platformName;
        this.mapper       = new ObjectMapper();
    }

    // Reads all posts using streaming mode
    // Instead of loading the whole file we read one post at a time
    @Override
    public List<Post> readPosts(String filePath) {

        List<Post> posts = new ArrayList<>();

        try {
            // First try streaming approach for large files
            List<Post> streamedPosts = readPostsStreaming(filePath);

            if (!streamedPosts.isEmpty()) {
                return streamedPosts;
            }

            // Fallback: if streaming finds nothing try the tree approach
            // This handles simpler JSON structures
            return readPostsFallback(filePath);

        } catch (Exception e) {
            System.out.println("Error reading posts: " + e.getMessage());
        }

        return posts;
    }

    // STREAMING approach - reads the JSON file token by token
    // Never loads more than one post into memory at a time
    // Perfect for large files
    private List<Post> readPostsStreaming(String filePath) {

        List<Post> posts    = new ArrayList<>();
        int        count    = 0;

        try {
            // JsonParser is Jackson's streaming reader
            // It reads the file like a book - one word at a time
            JsonParser parser = mapper.getFactory()
                                      .createParser(new File(filePath));

            String currentArrayName = null;

            // These are the field names we look for that contain posts
            // Different apps use different names
            String[] postArrayNames = {
                "posts", "media", "data", "items",
                "feed", "tweets", "stories", "content"
            };

            while (!parser.isClosed()) {

                JsonToken token = parser.nextToken();
                if (token == null) break;

                // When we find a field name check if it is a posts array
                if (token == JsonToken.FIELD_NAME) {
                    currentArrayName = parser.currentName();
                }

                // When we find the start of an array that looks like posts
                if (token == JsonToken.START_ARRAY
                        && currentArrayName != null
                        && isPostsArray(currentArrayName, postArrayNames)) {

                    // Now read each item inside this array one by one
                    while (parser.nextToken() != JsonToken.END_ARRAY) {

                        // Read just this one post into memory
                        JsonNode postNode = mapper.readTree(parser);
                        Post post         = extractPost(postNode);
                        posts.add(post);
                        count++;

                        // Show progress every 100 posts
                        // so user knows something is happening
                        if (count % BATCH_SIZE == 0) {
                            System.out.println("  Posts read so far: " + count);
                        }
                    }
                }
            }

            parser.close();
            System.out.println("Total posts read: " + count);

        } catch (Exception e) {
            System.out.println("Streaming read error: " + e.getMessage());
        }

        return posts;
    }

    // FALLBACK approach - used for simple flat JSON files
    // Only used when streaming finds nothing
    // Still memory efficient for smaller files
    private List<Post> readPostsFallback(String filePath) {

        List<Post> posts = new ArrayList<>();

        try {
            JsonNode root      = mapper.readTree(new File(filePath));
            JsonNode postsNode = findNode(root,
                "posts", "media", "data", "items", "feed");

            if (postsNode == null || !postsNode.isArray()) {
                System.out.println("No posts array found in file.");
                return posts;
            }

            // Process in batches to keep memory low
            List<Thread> threads = new ArrayList<>();
            int          count   = 0;

            for (JsonNode postNode : postsNode) {

                Thread t = Thread.ofVirtual().start(() -> {
                    Post post = extractPost(postNode);
                    synchronized (posts) {
                        posts.add(post);
                    }
                });

                threads.add(t);
                count++;

                // Every BATCH_SIZE posts wait for threads to finish
                // then clear the thread list to free memory
                if (count % BATCH_SIZE == 0) {
                    for (Thread thread : threads) {
                        thread.join();
                    }
                    threads.clear();
                    System.out.println("  Posts processed: " + count);
                }
            }

            // Wait for any remaining threads
            for (Thread t : threads) {
                t.join();
            }

        } catch (Exception e) {
            System.out.println("Fallback read error: " + e.getMessage());
        }

        return posts;
    }

    // Reads the user profile
    // Profile is usually small so we read it normally
    @Override
    public Profile readProfile(String filePath) {

        try {
            // Profile section is always small - safe to read directly
            JsonNode root        = mapper.readTree(new File(filePath));
            JsonNode profileNode = findNode(root,
                "profile", "user", "account", "personal_info");

            if (profileNode != null) {
                return extractProfile(profileNode);
            }

            return extractProfile(root);

        } catch (Exception e) {
            System.out.println("Error reading profile: " + e.getMessage());
        }

        return null;
    }

    @Override
    public String getPlatformName() {
        return platformName;
    }

    // ── HELPER METHODS ───────────────────────────────────────────────

    // Checks if an array field name is likely to contain posts
    private boolean isPostsArray(String name, String[] postArrayNames) {
        for (String postName : postArrayNames) {
            if (name.equalsIgnoreCase(postName)) return true;
        }
        return false;
    }

    // Looks for a child node by trying multiple possible names
    private JsonNode findNode(JsonNode root, String... possibleNames) {
        for (String name : possibleNames) {
            if (root.has(name)) return root.get(name);
        }
        return null;
    }

    // Pulls out one post from a JSON node
    private Post extractPost(JsonNode node) {
        String content   = getField(node,
            "caption", "text", "content", "description", "post", "full_text");
        String date      = getField(node,
            "taken_at", "date", "timestamp", "created_at", "time", "created_timestamp");
        String mediaType = getField(node,
            "media_type", "type", "kind", "format");

        return new Post(platformName, content, date, mediaType);
    }

    // Pulls out profile info from a JSON node
    private Profile extractProfile(JsonNode node) {
        String username = getField(node,
            "username", "handle", "screen_name", "user_name");
        String fullName = getField(node,
            "full_name", "name", "display_name", "real_name");
        String email    = getField(node,
            "email", "email_address", "contact_email");
        String bio      = getField(node,
            "bio", "biography", "about", "description", "summary");

        return new Profile(platformName, username, fullName, email, bio);
    }

    // Gets a field value by trying multiple possible names
    private String getField(JsonNode node, String... possibleNames) {
        for (String name : possibleNames) {
            if (node.has(name) && !node.get(name).isNull()) {
                return node.get(name).asText();
            }
        }
        return "unknown";
    }
}