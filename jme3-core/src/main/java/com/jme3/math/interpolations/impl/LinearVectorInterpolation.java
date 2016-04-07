package com.jme3.math.interpolations.impl;

import com.jme3.math.VectorNf;
import com.jme3.math.interpolations.api.NonPrimitiveInterpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class LinearVectorInterpolation<V extends VectorNf> implements NonPrimitiveInterpolation<V> {
    private final LinearFloatInterpolation[] inters;
    private final V lowerBound;
    private final V upperBound;

    public LinearVectorInterpolation(V start, V end) {

        this.lowerBound = start;
        this.upperBound = end;

        inters = new LinearFloatInterpolation[start.size()];

        for (int i = 0; i < start.size(); i++) {
            inters[i] = new LinearFloatInterpolation(start.get(i), end.get(i));
        }
    }

    @Override
    public void interpolate(float value, V resVector) {
        for (int i = 0; i < resVector.size(); i++) {
            resVector.set(i, inters[i].interpolate(value));
        }
    }
}
