package com.jme3.math.interpolations.impl;

import com.jme3.math.interpolations.api.Interpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class BezierFloatInterpolation implements Interpolation<Float> {
    private final float p0, p1, p2, p3;

    public BezierFloatInterpolation(float p0, float p1, float p2, float p3) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;
    }

    @Override
    public Float interpolate(float value) {
        float oneMinusValue = 1.0f - value;
        float oneMinusValue2 = oneMinusValue * oneMinusValue;
        float value2 = value * value;
        return p0 * oneMinusValue2 * oneMinusValue
                + 3.0f * p1 * value * oneMinusValue2
                + 3.0f * p2 * value2 * oneMinusValue
                + p3 * value2 * value;
    }
}
