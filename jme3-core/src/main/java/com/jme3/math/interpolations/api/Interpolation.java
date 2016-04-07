package com.jme3.math.interpolations.api;

/**
 * Created by lennart on 07/04/16.
 */
public interface Interpolation<T> {
    T interpolate(float value);
}
