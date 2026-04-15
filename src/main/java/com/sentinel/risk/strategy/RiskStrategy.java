package com.sentinel.risk.strategy;

import com.sentinel.risk.model.PostRiskResult;

// Strategy Pattern interface
// Every risk analysis rubric implements this
// Adding a new rubric = new class, zero changes elsewhere
public interface RiskStrategy {

    // Analyses one post and writes results into the PostRiskResult object
    // Uses void so multiple strategies can each enrich the same result object
    void analyse(String content, PostRiskResult result);

    // Human-readable name for logging
    String getStrategyName();
}