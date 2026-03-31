package com.sentinel.model;

import java.util.List;

// This class represents the final clean output of our entire pipeline
// This is what gets saved as a JSON file and handed to the next component
public class OutputReport {

    private String reportId;      // unique number e.g. "001"
    private String platform;      // which app the data came from
    private String generatedAt;   // when this report was created
    private Profile profile;      // the cleaned profile
    private List<Post> posts;     // the cleaned list of posts
    private int totalPosts;       // how many posts were found
    private int skippedPosts;     // how many posts were skipped

    // Empty constructor - needed by Jackson to write JSON
    public OutputReport() {}

    // Constructor to build the report all at once
    public OutputReport(String reportId, String platform, String generatedAt,
                        Profile profile, List<Post> posts,
                        int totalPosts, int skippedPosts) {
        this.reportId      = reportId;
        this.platform      = platform;
        this.generatedAt   = generatedAt;
        this.profile       = profile;
        this.posts         = posts;
        this.totalPosts    = totalPosts;
        this.skippedPosts  = skippedPosts;
    }

    // Getters
    public String getReportId()     { return reportId; }
    public String getPlatform()     { return platform; }
    public String getGeneratedAt()  { return generatedAt; }
    public Profile getProfile()     { return profile; }
    public List<Post> getPosts()    { return posts; }
    public int getTotalPosts()      { return totalPosts; }
    public int getSkippedPosts()    { return skippedPosts; }

    // Setters
    public void setReportId(String reportId)         { this.reportId = reportId; }
    public void setPlatform(String platform)         { this.platform = platform; }
    public void setGeneratedAt(String generatedAt)   { this.generatedAt = generatedAt; }
    public void setProfile(Profile profile)          { this.profile = profile; }
    public void setPosts(List<Post> posts)           { this.posts = posts; }
    public void setTotalPosts(int totalPosts)         { this.totalPosts = totalPosts; }
    public void setSkippedPosts(int skippedPosts)     { this.skippedPosts = skippedPosts; }
}