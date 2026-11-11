package com.skyflow.detect;

/**
 * Processing metrics returned by Detect operations.
 */
public final class Metrics {
    private final Double size;
    private final Integer wordCount;
    private final Integer characterCount;
    private final Integer slides;
    private final Integer pages;
    private final Double duration;

    public Metrics(Double size, Integer wordCount, Integer characterCount, Integer slides, Integer pages, Double duration) {
        this.size = size;
        this.wordCount = wordCount;
        this.characterCount = characterCount;
        this.slides = slides;
        this.pages = pages;
        this.duration = duration;
    }

    /**
     * @return Input size in kilobytes.
     */
    public Double getSize() {
        return size;
    }

    /**
     * @return Number of words.
     */
    public Integer getWordCount() {
        return wordCount;
    }

    /**
     * @return Number of characters.
     */
    public Integer getCharacterCount() {
        return characterCount;
    }

    /**
     * @return Number of slides.
     */
    public Integer getSlides() {
        return slides;
    }

    /**
     * @return Number of pages.
     */
    public Integer getPages() {
        return pages;
    }

    /**
     * @return Media duration in seconds.
     */
    public Double getDuration() {
        return duration;
    }

    @Override
    public String toString() {
        return "Metrics{" +
                "size=" + size +
                ", wordCount=" + wordCount +
                ", characterCount=" + characterCount +
                ", slides=" + slides +
                ", pages=" + pages +
                ", duration=" + duration +
                '}';
    }
}
