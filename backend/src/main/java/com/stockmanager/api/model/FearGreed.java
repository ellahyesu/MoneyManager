package com.stockmanager.api.model;

public class FearGreed {
    private final int value;
    private final String classification;

    public FearGreed(int value, String classification) {
        this.value = value;
        this.classification = classification;
    }

    public int getValue() {
        return value;
    }

    public String getClassification() {
        return classification;
    }
}

