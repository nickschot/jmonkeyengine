package com.jme3.math.interpolations.impl;

import com.jme3.math.interpolations.api.Interpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class LinearFloatInterpolation implements Interpolation<Float> {

    private final float start;
    private final float end;

    public LinearFloatInterpolation(float start, float end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public Float interpolate(float step) {
        if (this.start == this.end) {
            return this.start;
        }
        if (step <= 0f) {
            return this.start;
        }
        if (step >= 1f) {
            return this.end;
        }
        return ((1f - step) * this.start) + (step * this.end);

    }
}
