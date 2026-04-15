package com.sentinel.risk.model;

import java.util.ArrayList;
import java.util.List;

// Holds the risk analysis result for one single post
public class PostRiskResult {

    private String       content;          // the original post text
    private String       date;             // when it was posted
    private String       mediaType;        // post / story / comment
    private double       toxicityScore;    // 0.0 to 1.0
    private double       sentimentScore;   // -1.0 (negative) to +1.0 (positive)
    private boolean      piiDetected;      // true if any PII found
    private List<String> piiTypes;         // e.g. ["EMAIL", "PHONE", "ADDRESS"]
    private List<String> flags;            // e.g. ["TOXIC", "PII_LEAK", "NEGATIVE_SENTIMENT"]

    public PostRiskResult(String content, String date, String mediaType) {
        this.content       = content;
        this.date          = date;
        this.mediaType     = mediaType;
        this.toxicityScore = 0.0;
        this.sentimentScore = 0.0;
        this.piiDetected   = false;
        this.piiTypes      = new ArrayList<>();
        this.flags         = new ArrayList<>();
    }

    public void addFlag(String flag) {
        if (!flags.contains(flag)) flags.add(flag);
    }

    public void addPiiType(String piiType) {
        if (!piiTypes.contains(piiType)) piiTypes.add(piiType);
        this.piiDetected = true;
    }

    // Getters
    public String       getContent()        { return content; }
    public String       getDate()           { return date; }
    public String       getMediaType()      { return mediaType; }
    public double       getToxicityScore()  { return toxicityScore; }
    public double       getSentimentScore() { return sentimentScore; }
    public boolean      isPiiDetected()     { return piiDetected; }
    public List<String> getPiiTypes()       { return piiTypes; }
    public List<String> getFlags()          { return flags; }

    // Setters
    public void setToxicityScore(double toxicityScore)   { this.toxicityScore = toxicityScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
    public void setPiiDetected(boolean piiDetected)      { this.piiDetected = piiDetected; }
}