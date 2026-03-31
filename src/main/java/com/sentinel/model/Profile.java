package com.sentinel.model;

// This class stores the basic profile info of the user
// from whatever social media app they uploaded
public class Profile {

    private String platform;   // which app this came from e.g. "linkedin"
    private String username;   // their username or handle
    private String fullName;   // their full name
    private String email;      // their email if available in the export
    private String bio;        // their profile bio or description

    // Empty constructor - needed by Jackson to read JSON
    public Profile() {}

    // Constructor to create a profile with all details at once
    public Profile(String platform, String username, String fullName, String email, String bio) {
        this.platform = platform;
        this.username = username;
        this.fullName = fullName;
        this.email    = email;
        this.bio      = bio;
    }

    // Getters - used to READ the value of each field
    public String getPlatform() { return platform; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getEmail()    { return email; }
    public String getBio()      { return bio; }

    // Setters - used to SET the value of each field
    public void setPlatform(String platform) { this.platform = platform; }
    public void setUsername(String username) { this.username = username; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setEmail(String email)       { this.email = email; }
    public void setBio(String bio)           { this.bio = bio; }

    // Shows the profile as readable text when we print it
    @Override
    public String toString() {
        return "[" + platform + "] " + fullName + " (@" + username + ") | " + email;
    }
}