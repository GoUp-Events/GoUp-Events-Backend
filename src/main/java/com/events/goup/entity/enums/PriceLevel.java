package com.events.goup.entity.enums;

public enum PriceLevel {
    FREE,
    INEXPENSIVE,
    MODERATE,
    EXPENSIVE,
    VERY_EXPENSIVE;

    // Google Places (API New) devolve "PRICE_LEVEL_MODERATE", "PRICE_LEVEL_UNSPECIFIED", etc.
    public static PriceLevel fromGoogle(String value) {
        if (value == null || !value.startsWith("PRICE_LEVEL_")) {
            return null;
        }
        try {
            return PriceLevel.valueOf(value.substring("PRICE_LEVEL_".length()));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
