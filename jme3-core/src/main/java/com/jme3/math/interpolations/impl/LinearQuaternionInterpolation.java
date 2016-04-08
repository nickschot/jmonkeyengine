package com.jme3.math.interpolations.impl;

import com.jme3.math.Quaternion;
import com.jme3.math.interpolations.api.NonPrimitiveInterpolation;

/**
 * Created by lennart on 08/04/16.
 */
public class LinearQuaternionInterpolation implements NonPrimitiveInterpolation<Quaternion> {
    private final Quaternion start;
    private final Quaternion end;

    public LinearQuaternionInterpolation(Quaternion start, Quaternion end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void interpolate(float step, Quaternion res) {
        res.slerp(this.start, this.end, step);
    }
}
