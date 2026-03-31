package com.sentinel.model;

// This class represents one single post from any social media app
public class Post {

    private String platform;   // which app this came from e.g. "instagram"
    private String content;    // the text of the post
    private String date;       // when it was posted
    private String mediaType;  // was it a photo, video, text etc.

    // Empty constructor - needed by Jackson to read JSON
    public Post() {}

    // Constructor to create a post with all details at once
    public Post(String platform, String content, String date, String mediaType) {
        this.platform  = platform;
        this.content   = content;
        this.date      = date;
        this.mediaType = mediaType;
    }

    // Getters - used to READ the value of each field
    public String getPlatform()  { return platform; }
    public String getContent()   { return content; }
    public String getDate()      { return date; }
    public String getMediaType() { return mediaType; }

    // Setters - used to SET the value of each field
    public void setPlatform(String platform)   { this.platform = platform; }
    public void setContent(String content)     { this.content = content; }
    public void setDate(String date)           { this.date = date; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }

    // Shows the post as readable text when we print it
    @Override
    public String toString() {
        return "[" + platform + "] " + date + " | " + mediaType + " | " + content;
    }
}