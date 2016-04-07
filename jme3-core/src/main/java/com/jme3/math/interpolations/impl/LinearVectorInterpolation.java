package com.jme3.math.interpolations.impl;

import com.jme3.math.Vector2f;
import com.jme3.math.VectorNf;
import com.jme3.math.interpolations.api.Interpolation;
import com.jme3.math.interpolations.api.VectorInterpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class LinearVectorInterpolation<V extends VectorNf> implements VectorInterpolation<V> {
    private final LinearFloatInterpolation[] inters;

    public LinearVectorInterpolation(V start, V end) {

        inters = new LinearFloatInterpolation[start.size()];

        for (int i = 0; i < start.size(); i++) {
            inters[i] = new LinearFloatInterpolation(start.get(i), end.get(i));
        }
    }

    @Override
    public V interpolate(float value, V resVector) {
        for (int i = 0; i < resVector.size(); i++) {
            resVector.set(i, inters[i].interpolate(value));
        }

        return resVector;
    }
}
