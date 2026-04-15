package com.sentinel.risk.engine;

import com.sentinel.model.OutputReport;
import com.sentinel.model.Post;
import com.sentinel.model.Profile;
import com.sentinel.risk.model.PostRiskResult;
import com.sentinel.risk.model.RiskReport;
import com.sentinel.risk.strategy.PiiStrategy;
import com.sentinel.risk.strategy.RiskStrategy;
import com.sentinel.risk.strategy.SentimentStrategy;
import com.sentinel.risk.strategy.ToxicityStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// Orchestrates all risk strategies across every post and the profile
// Platform-independent - works on any OutputReport regardless of source
public class RiskEngine {

    // The three strategies - easily extendable by adding to this list
    private final List<RiskStrategy> strategies;

    // Patterns for profile-level PII scanning
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "(\\+91[\\s\\-]?|91[\\s\\-]?|0)?[6-9]\\d{9}"
    );
    private static final Pattern DOB_PATTERN = Pattern.compile(
        "DOB:\\s*\\d{4}-\\d{2}-\\d{2}"
    );

    public RiskEngine() {
        // Chain of strategies - order matters: PII first, then toxicity, then sentiment
        this.strategies = new ArrayList<>();
        this.strategies.add(new PiiStrategy());
        this.strategies.add(new ToxicityStrategy());
        this.strategies.add(new SentimentStrategy());
    }

    // Main entry point - enriches the OutputReport with a RiskReport
    public OutputReport analyse(OutputReport report) {

        System.out.println("\n==============================================");
        System.out.println("   Risk Analysis Engine");
        System.out.println("==============================================");

        List<Post>           posts   = report.getPosts();
        Profile              profile = report.getProfile();
        List<PostRiskResult> results = new ArrayList<>();

        // ── Analyse each post through all strategies ──────────────────
        System.out.println("Analysing " + posts.size() + " posts...");

        for (Post post : posts) {
            PostRiskResult result = new PostRiskResult(
                post.getContent(),
                post.getDate(),
                post.getMediaType()
            );

            // Run every strategy on this post
            // This is the Chain of Responsibility in action
            for (RiskStrategy strategy : strategies) {
                strategy.analyse(post.getContent(), result);
            }

            results.add(result);
        }

        // ── Analyse the profile for PII ───────────────────────────────
        System.out.println("Analysing profile...");
        RiskReport.ProfileRiskResult profileRisk = analyseProfile(profile);

        // ── Calculate summary counts ──────────────────────────────────
        int toxicCount    = 0;
        int piiCount      = 0;
        int negCount      = 0;
        int highRiskCount = 0;

        for (PostRiskResult r : results) {
            if (r.getFlags().contains("TOXIC") || r.getFlags().contains("THREAT"))
                toxicCount++;
            if (r.isPiiDetected())
                piiCount++;
            if (r.getFlags().contains("NEGATIVE_SENTIMENT"))
                negCount++;
            if (r.getToxicityScore() > 0.7 || r.isPiiDetected())
                highRiskCount++;
        }

        RiskReport.RiskSummary summary = new RiskReport.RiskSummary(
            posts.size(), toxicCount, piiCount, negCount, highRiskCount
        );

        // ── Calculate overall risk score (0 - 100) ────────────────────
        double overallScore = calculateOverallScore(results, profileRisk, posts.size());
        String riskLevel    = getRiskLevel(overallScore);

        System.out.println("Overall risk score : " + String.format("%.1f", overallScore));
        System.out.println("Risk level         : " + riskLevel);
        System.out.println("Toxic posts        : " + toxicCount);
        System.out.println("PII leak posts     : " + piiCount);
        System.out.println("Negative sentiment : " + negCount);
        System.out.println("==============================================");

        // ── Build the RiskReport and attach to OutputReport ───────────
        RiskReport riskReport = new RiskReport(
            overallScore, riskLevel, profileRisk, results, summary
        );

        report.setRiskReport(riskReport);
        return report;
    }

    // Checks profile fields directly for exposed PII
    private RiskReport.ProfileRiskResult analyseProfile(Profile profile) {

        List<String> flaggedFields = new ArrayList<>();

        if (profile == null) {
            return new RiskReport.ProfileRiskResult(false, flaggedFields);
        }

        // Email exposed in profile
        String email = profile.getEmail();
        if (email != null && !email.isBlank()
                && !email.startsWith("h***")
                && EMAIL_PATTERN.matcher(email).find()) {
            flaggedFields.add("email");
        }

        // Phone in bio
        String bio = profile.getBio();
        if (bio != null && PHONE_PATTERN.matcher(bio).find()) {
            flaggedFields.add("bio_phone");
        }

        // DOB in bio - a privacy risk
        if (bio != null && DOB_PATTERN.matcher(bio).find()) {
            flaggedFields.add("bio_dob");
        }

        // Full name exposed
        String fullName = profile.getFullName();
        if (fullName != null && !fullName.equals("unknown")
                && !fullName.isBlank()) {
            flaggedFields.add("fullName");
        }

        return new RiskReport.ProfileRiskResult(!flaggedFields.isEmpty(), flaggedFields);
    }

    // Weighted scoring formula
    // Toxicity contributes 40%, PII 35%, sentiment 25%
    private double calculateOverallScore(List<PostRiskResult> results,
                                         RiskReport.ProfileRiskResult profileRisk,
                                         int totalPosts) {
        if (totalPosts == 0) return 0.0;

        double toxicitySum  = 0.0;
        double piiSum       = 0.0;
        double sentimentSum = 0.0;

        for (PostRiskResult r : results) {
            toxicitySum  += r.getToxicityScore();
            piiSum       += r.isPiiDetected() ? 1.0 : 0.0;
            sentimentSum += (1.0 - r.getSentimentScore()) / 2.0;
        }

        double avgToxicity  = toxicitySum  / totalPosts;
        double avgPii       = piiSum       / totalPosts;
        double avgSentiment = sentimentSum / totalPosts;

        // Profile PII adds a flat 10 point bonus to the score
        double profileBonus = profileRisk.isPiiExposed() ? 10.0 : 0.0;

        double score = (avgToxicity  * 40.0)
                     + (avgPii       * 35.0)
                     + (avgSentiment * 25.0)
                     + profileBonus;

        return Math.min(score, 100.0);
    }

    // Converts numeric score to a human-readable risk level
    private String getRiskLevel(double score) {
        if (score >= 70.0) return "CRITICAL";
        if (score >= 45.0) return "HIGH";
        if (score >= 20.0) return "MEDIUM";
        return "LOW";
    }
}