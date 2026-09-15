package com.events.goup.entity.enums;

public enum AgeRating {
    FREE("Livre"),
    TEN("10 anos"),
    TWELVE("12 anos"),
    FOURTEEN("14 anos"),
    SIXTEEN("16 anos"),
    EIGHTEEN("18 anos");

    private final String label;

    AgeRating(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}