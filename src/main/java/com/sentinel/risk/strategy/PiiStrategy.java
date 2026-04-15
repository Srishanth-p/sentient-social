package com.sentinel.risk.strategy;

import com.sentinel.risk.model.PostRiskResult;

import java.util.regex.Pattern;

// Detects Personally Identifiable Information in post content
// Uses regex - no external dependencies needed
// Covers: email, phone, physical address, Aadhaar, credit card, salary
public class PiiStrategy implements RiskStrategy {

    // Email pattern - matches standard email formats
    private static final Pattern EMAIL = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}",
        Pattern.CASE_INSENSITIVE
    );

    // Indian phone number - 10 digits, optionally prefixed with +91 or 91
    private static final Pattern PHONE = Pattern.compile(
        "(\\+91[\\s\\-]?|91[\\s\\-]?|0)?[6-9]\\d{9}"
    );

    // Indian Aadhaar - 12 digits in groups of 4
    private static final Pattern AADHAAR = Pattern.compile(
        "\\b\\d{4}\\s\\d{4}\\s\\d{4}\\b"
    );

    // Credit card - 16 digits, optionally space/dash separated
    private static final Pattern CREDIT_CARD = Pattern.compile(
        "\\b(?:\\d[ \\-]?){15,16}\\d\\b"
    );

    // Physical address indicators - keywords that suggest an address is present
    private static final Pattern ADDRESS = Pattern.compile(
        "(?i)(\\d+[A-Z]?[,\\s]+[\\w\\s]+(?:street|st|road|rd|avenue|ave|block|" +
        "nagar|layout|colony|sector|phase|cross|main)[,\\s]+[\\w\\s]+\\d{5,6})"
    );

    // Salary disclosure - e.g. "45 LPA", "18-30 LPA", "salary is X"
    private static final Pattern SALARY = Pattern.compile(
        "(?i)(\\d+[\\s\\-]*(?:to[\\s\\-]*\\d+)?\\s*lpa|salary\\s+(is|of|range)?\\s*\\d+)"
    );

    @Override
    public String getStrategyName() {
        return "PII Detection";
    }

    @Override
    public void analyse(String content, PostRiskResult result) {

        if (content == null || content.isBlank()) return;

        if (EMAIL.matcher(content).find()) {
            result.addPiiType("EMAIL");
            result.addFlag("PII_LEAK");
        }

        if (PHONE.matcher(content).find()) {
            result.addPiiType("PHONE");
            result.addFlag("PII_LEAK");
        }

        if (AADHAAR.matcher(content).find()) {
            result.addPiiType("AADHAAR");
            result.addFlag("PII_LEAK");
            result.addFlag("SENSITIVE_ID");
        }

        if (CREDIT_CARD.matcher(content).find()) {
            result.addPiiType("CREDIT_CARD");
            result.addFlag("PII_LEAK");
            result.addFlag("FINANCIAL_DATA");
        }

        if (ADDRESS.matcher(content).find()) {
            result.addPiiType("ADDRESS");
            result.addFlag("PII_LEAK");
        }

        if (SALARY.matcher(content).find()) {
            result.addPiiType("SALARY");
            result.addFlag("FINANCIAL_DATA");
        }
    }
}