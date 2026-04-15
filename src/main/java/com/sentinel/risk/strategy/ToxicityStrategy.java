package com.sentinel.risk.strategy;

import com.sentinel.risk.model.PostRiskResult;

import java.util.List;

// Detects toxic, hateful, threatening, or professionally damaging language
// Uses a weighted keyword approach grouped by severity tier
// Score is calculated as: sum of matched weights / max possible weight, capped at 1.0
public class ToxicityStrategy implements RiskStrategy {

    // Tier 3 — CRITICAL: direct threats, severe harassment (weight 1.0 each)
    private static final List<String> TIER_3 = List.of(
        "i will destroy", "i will kill", "you will regret",
        "messed with the wrong", "i know where you live",
        "hope you die", "end yourself"
    );

    // Tier 2 — HIGH: insults, strong negativity, reputation attacks (weight 0.6 each)
    private static final List<String> TIER_2 = List.of(
        "stupid", "idiot", "trash", "disgrace", "fraud",
        "absolute clown", "dumb as a rock", "useless",
        "hate people like you", "make me sick", "just quit",
        "complete idiot", "toxic hellhole", "fake", "coward"
    );

    // Tier 1 — MEDIUM: unprofessional negativity (weight 0.3 each)
    private static final List<String> TIER_1 = List.of(
        "sick of", "cannot stand", "drains me", "pointless",
        "name and shame", "absolute disgrace", "fails spectacularly",
        "nobody cares", "showing off", "out for themselves"
    );

    private static final double WEIGHT_TIER_3 = 1.0;
    private static final double WEIGHT_TIER_2 = 0.6;
    private static final double WEIGHT_TIER_1 = 0.3;
    private static final double THRESHOLD_FLAG = 0.4;   // flag as TOXIC above this score

    @Override
    public String getStrategyName() {
        return "Toxicity Detection";
    }

    @Override
    public void analyse(String content, PostRiskResult result) {

        if (content == null || content.isBlank()) return;

        String lower = content.toLowerCase();
        double score = 0.0;

        for (String keyword : TIER_3) {
            if (lower.contains(keyword)) {
                score += WEIGHT_TIER_3;
                result.addFlag("THREAT");
            }
        }

        for (String keyword : TIER_2) {
            if (lower.contains(keyword)) {
                score += WEIGHT_TIER_2;
            }
        }

        for (String keyword : TIER_1) {
            if (lower.contains(keyword)) {
                score += WEIGHT_TIER_1;
            }
        }

        // Normalise to 0.0 - 1.0
        double normalised = Math.min(score / 2.0, 1.0);
        result.setToxicityScore(normalised);

        if (normalised >= THRESHOLD_FLAG) {
            result.addFlag("TOXIC");
        } else if (normalised > 0.1) {
            result.addFlag("MILDLY_NEGATIVE");
        }
    }
}