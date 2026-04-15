package com.sentinel.risk.model;

import java.util.List;

// The complete risk analysis report for the entire OutputReport
// This gets added as a new field inside OutputReport
public class RiskReport {

    private double            overallRiskScore;   // 0.0 to 100.0
    private String            riskLevel;          // LOW / MEDIUM / HIGH / CRITICAL
    private ProfileRiskResult profileRisk;        // risk findings on the profile itself
    private List<PostRiskResult> postResults;     // per-post breakdown
    private RiskSummary       summary;            // aggregated counts

    public RiskReport() {}

    public RiskReport(double overallRiskScore, String riskLevel,
                      ProfileRiskResult profileRisk,
                      List<PostRiskResult> postResults,
                      RiskSummary summary) {
        this.overallRiskScore = overallRiskScore;
        this.riskLevel        = riskLevel;
        this.profileRisk      = profileRisk;
        this.postResults      = postResults;
        this.summary          = summary;
    }

    // Getters
    public double               getOverallRiskScore() { return overallRiskScore; }
    public String               getRiskLevel()        { return riskLevel; }
    public ProfileRiskResult    getProfileRisk()      { return profileRisk; }
    public List<PostRiskResult> getPostResults()      { return postResults; }
    public RiskSummary          getSummary()          { return summary; }

    // Setters
    public void setOverallRiskScore(double overallRiskScore) { this.overallRiskScore = overallRiskScore; }
    public void setRiskLevel(String riskLevel)               { this.riskLevel = riskLevel; }
    public void setProfileRisk(ProfileRiskResult profileRisk){ this.profileRisk = profileRisk; }
    public void setPostResults(List<PostRiskResult> postResults) { this.postResults = postResults; }
    public void setSummary(RiskSummary summary)               { this.summary = summary; }

    // ── Nested model classes ──────────────────────────────────────────

    public static class ProfileRiskResult {
        private boolean      piiExposed;
        private List<String> flaggedFields;   // e.g. ["email", "bio", "dob"]

        public ProfileRiskResult(boolean piiExposed, List<String> flaggedFields) {
            this.piiExposed    = piiExposed;
            this.flaggedFields = flaggedFields;
        }

        public boolean      isPiiExposed()    { return piiExposed; }
        public List<String> getFlaggedFields(){ return flaggedFields; }
        public void setPiiExposed(boolean piiExposed)         { this.piiExposed = piiExposed; }
        public void setFlaggedFields(List<String> flaggedFields){ this.flaggedFields = flaggedFields; }
    }

    public static class RiskSummary {
        private int totalPostsAnalysed;
        private int toxicPostCount;
        private int piiLeakCount;
        private int negativeSentimentCount;
        private int highRiskPostCount;    // posts with score > 0.7

        public RiskSummary(int totalPostsAnalysed, int toxicPostCount,
                           int piiLeakCount, int negativeSentimentCount,
                           int highRiskPostCount) {
            this.totalPostsAnalysed    = totalPostsAnalysed;
            this.toxicPostCount        = toxicPostCount;
            this.piiLeakCount          = piiLeakCount;
            this.negativeSentimentCount = negativeSentimentCount;
            this.highRiskPostCount     = highRiskPostCount;
        }

        public int getTotalPostsAnalysed()     { return totalPostsAnalysed; }
        public int getToxicPostCount()         { return toxicPostCount; }
        public int getPiiLeakCount()           { return piiLeakCount; }
        public int getNegativeSentimentCount() { return negativeSentimentCount; }
        public int getHighRiskPostCount()      { return highRiskPostCount; }

        public void setTotalPostsAnalysed(int v)     { this.totalPostsAnalysed = v; }
        public void setToxicPostCount(int v)         { this.toxicPostCount = v; }
        public void setPiiLeakCount(int v)           { this.piiLeakCount = v; }
        public void setNegativeSentimentCount(int v) { this.negativeSentimentCount = v; }
        public void setHighRiskPostCount(int v)      { this.highRiskPostCount = v; }
    }
}