package com.jme3.math.interpolations.impl;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.VectorNf;
import com.jme3.math.interpolations.api.NonPrimitiveInterpolation;

/**
 * Created by lennart on 07/04/16.
 */
public class BezierVectorInterpolation<V extends VectorNf>  implements NonPrimitiveInterpolation<V> {
    private final BezierFloatInterpolation[] inters;

    private final V p0;
    private final V p1;
    private final V p2;
    private final V p3;


    public BezierVectorInterpolation(V p0, V p1, V p2, V p3) {
        this.p0 = p0;
        this.p1 = p1;
        this.p2 = p2;
        this.p3 = p3;

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
