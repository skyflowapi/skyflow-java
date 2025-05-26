package com.skyflow.vault.detect;


public class TextIndex {
    private final int start;
    private final int end;

    public TextIndex(int start, int end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        return "TextIndex{" +
                "start=" + start +
                ", end=" + end +
                '}';
    }
}
