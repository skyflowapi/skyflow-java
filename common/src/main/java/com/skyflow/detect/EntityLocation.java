package com.skyflow.detect;

/**
 * Position of a detected entity in the original and processed text.
 */
public final class EntityLocation {
    private final Integer startIndex;
    private final Integer endIndex;
    private final Integer startIndexProcessed;
    private final Integer endIndexProcessed;

    public EntityLocation(Integer startIndex, Integer endIndex, Integer startIndexProcessed, Integer endIndexProcessed) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.startIndexProcessed = startIndexProcessed;
        this.endIndexProcessed = endIndexProcessed;
    }

    /**
     * @return Start index in the original text.
     */
    public Integer getStartIndex() {
        return startIndex;
    }

    /**
     * @return End index in the original text.
     */
    public Integer getEndIndex() {
        return endIndex;
    }

    /**
     * @return Start index in the processed text.
     */
    public Integer getStartIndexProcessed() {
        return startIndexProcessed;
    }

    /**
     * @return End index in the processed text.
     */
    public Integer getEndIndexProcessed() {
        return endIndexProcessed;
    }

    @Override
    public String toString() {
        return "EntityLocation{" +
                "startIndex=" + startIndex +
                ", endIndex=" + endIndex +
                ", startIndexProcessed=" + startIndexProcessed +
                ", endIndexProcessed=" + endIndexProcessed +
                '}';
    }
}
