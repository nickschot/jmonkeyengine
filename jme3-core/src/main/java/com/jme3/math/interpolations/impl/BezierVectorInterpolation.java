package com.jme3.math.interpolations.impl;

import com.jme3.math.VectorNf;
import com.jme3.math.interpolations.api.NonPrimitiveInterpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class BezierVectorInterpolation<V extends VectorNf>  implements NonPrimitiveInterpolation<V> {
    private final BezierFloatInterpolation[] inters;

    private final V lowerBound;
    private final V upperBound;

    public BezierVectorInterpolation(V p0, V p1, V p2, V p3) {

        this.lowerBound = p0;
        this.upperBound = p3;

        inters = new BezierFloatInterpolation[p0.size()];

        for (int i = 0; i < p0.size(); i++) {
            inters[i] = new BezierFloatInterpolation(p0.get(i), p1.get(i), p2.get(i), p3.get(i));
        }

    }

    @Override
    public void interpolate(float step, V resVector) {
        for (int i = 0; i < resVector.size(); i++) {
            resVector.set(i, inters[i].interpolate(step));
        }
    }
}
