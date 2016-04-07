package com.jme3.math.interpolations.impl;

import com.jme3.math.interpolations.api.Interpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class CatmullRomFloatInterpolation implements Interpolation<Float> {
    private final float lowerBound;
    private final float upperBound;
    private float c1, c2, c3, c4;


    public CatmullRomFloatInterpolation(float T, float p0, float p1, float p2, float p3) {
        this.lowerBound = p1;
        this.upperBound = p2;

        c1 = p1;
        c2 = -1.0f * T * p0 + T * p2;
        c3 = 2 * T * p0 + (T - 3) * p1 + (3 - 2 * T) * p2 + -T * p3;
        c4 = -T * p0 + (2 - T) * p1 + (T - 2) * p2 + T * p3;
    }

    @Override
    public Float interpolate(float value) {
        return (float) (((c4 * value + c3) * value + c2) * value + c1);
    }
}
