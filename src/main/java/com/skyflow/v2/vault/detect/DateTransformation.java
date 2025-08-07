package com.skyflow.v2.vault.detect;


import com.skyflow.v2.enums.DetectEntities;

import java.util.List;

public class DateTransformation {
    private final int max;
    private final int min;
    private final List<DetectEntities> entities;

    public DateTransformation(int max, int min, List<DetectEntities> entities) {
        this.max = max;
        this.min = min;
        this.entities = entities;
    }

    public int getMax() {
        return max;
    }

    public int getMin() {
        return min;
    }

    public List<DetectEntities> getEntities() {
        return entities;
    }
}
