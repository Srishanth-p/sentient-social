package com.sentinel.risk.strategy;

import com.sentinel.risk.model.PostRiskResult;

import java.util.List;

// Analyses the professional sentiment of post content
// Score: -1.0 = very negative, 0.0 = neutral, +1.0 = very positive
// Focuses on professional context - not just general positivity/negativity
public class SentimentStrategy implements RiskStrategy {

    // Positive professional signals (weight +0.4 each)
    private static final List<String> POSITIVE_STRONG = List.of(
        "thrilled to announce", "excited to", "proud of", "grateful",
        "incredible", "amazing", "congrats", "well deserved",
        "best team", "great work", "love working", "inspiring"
    );

    // Positive mild signals (weight +0.2 each)
    private static final List<String> POSITIVE_MILD = List.of(
        "great post", "insightful", "bookmarking", "love this",
        "well written", "looking forward", "happy to", "beautiful",
        "shipped", "launched", "open sourced", "contributions welcome"
    );

    // Negative professional signals (weight -0.4 each)
    private static final List<String> NEGATIVE_STRONG = List.of(
        "hate", "sick of", "cannot stand", "drains me", "fake",
        "out for themselves", "absolute clown", "complete idiot",
        "toxic", "useless", "disgrace", "fraud", "quit already"
    );

    // Negative mild signals (weight -0.2 each)
    private static final List<String> NEGATIVE_MILD = List.of(
        "unfortunately", "disappointed", "frustrating", "annoying",
        "pointless", "nobody cares", "not interested", "tired of",
        "cowardly", "unprofessional", "name and shame"
    );

    private static final double NEGATIVE_THRESHOLD = -0.3;  // flag below this

    @Override
    public String getStrategyName() {
        return "Sentiment Analysis";
    }

    @Override
    public void analyse(String content, PostRiskResult result) {

        if (content == null || content.isBlank()) return;

        String lower = content.toLowerCase();
        double score = 0.0;

        for (String phrase : POSITIVE_STRONG) {
            if (lower.contains(phrase)) score += 0.4;
        }
        for (String phrase : POSITIVE_MILD) {
            if (lower.contains(phrase)) score += 0.2;
        }
        for (String phrase : NEGATIVE_STRONG) {
            if (lower.contains(phrase)) score -= 0.4;
        }
        for (String phrase : NEGATIVE_MILD) {
            if (lower.contains(phrase)) score -= 0.2;
        }

        // Clamp to -1.0 to +1.0
        score = Math.max(-1.0, Math.min(1.0, score));
        result.setSentimentScore(score);

        if (score <= NEGATIVE_THRESHOLD) {
            result.addFlag("NEGATIVE_SENTIMENT");
        } else if (score >= 0.3) {
            result.addFlag("POSITIVE_SENTIMENT");
        }
    }
}