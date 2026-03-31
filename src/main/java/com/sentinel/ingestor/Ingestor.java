package com.sentinel.ingestor;

import com.sentinel.model.Post;
import com.sentinel.model.Profile;
import java.util.List;

// This is an INTERFACE - think of it as a rule book
// Every ingestor (Instagram, LinkedIn, any new app) MUST follow these rules
// This is the STRATEGY PATTERN from OOAD - each app has its own way of reading
// data but they all follow the same contract defined here
public interface Ingestor {

    // Every ingestor must be able to read posts from a file
    // filePath = the location of the uploaded JSON file on the computer
    // returns a list of Post objects
    List<Post> readPosts(String filePath);

    // Every ingestor must be able to read the profile from a file
    // returns one Profile object
    Profile readProfile(String filePath);

    // Every ingestor must be able to say which app it handles
    // e.g. returns "instagram" or "linkedin"
    String getPlatformName();

}