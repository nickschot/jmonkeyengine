package com.jme3.math.interpolations.api;

/**
 * Created by lennart on 07/04/16.
 */
public interface Interpolation<T> {

    /** Applies the Interpolation function on a certain value
     *
     * @param step The step in range of the interpolator
     * @return The result of applying the interpolation strategy on the given step
     */
    T interpolate(float step);
}
