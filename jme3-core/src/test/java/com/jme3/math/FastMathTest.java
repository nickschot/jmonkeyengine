/*
 * Copyright (c) 2009-2015 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.jme3.math;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


/**
 * Verifies that algorithms in {@link FastMath} are working correctly.
 * 
 * @author Kirill Vainer
 */
public class FastMathTest {
    
    private int nearestPowerOfTwoSlow(int number) {
        return (int) Math.pow(2, Math.ceil(Math.log(number) / Math.log(2)));
    }
    
    @Test
    public void testNearestPowerOfTwo() {
        for (int i = -100; i < 1; i++) {
            assert FastMath.nearestPowerOfTwo(i) == 1;
        }
        for (int i = 1; i < 10000; i++) {
            int nextPowerOf2 = FastMath.nearestPowerOfTwo(i);
            assert i <= nextPowerOf2;
            assert FastMath.isPowerOfTwo(nextPowerOf2);
            assert nextPowerOf2 == nearestPowerOfTwoSlow(i);
        }
    }

    @Test
    public void testLinearInterpolate() {
        // Some good weather situations
        assertEquals(60.0f, FastMath.interpolateLinear(0.1f, 50.0f, 150.0f),  0.001f);
        assertEquals(100.0f, FastMath.interpolateLinear(0.5f, 50.0f, 150.0f),  0.001f);
        assertEquals(70.0f, FastMath.interpolateLinear(0.2f, 50.0f, 150.0f),  0.001f);

        // Some bad weather situations
        // On the border of the range
        assertEquals(50.0f, FastMath.interpolateLinear(0.0f, 50.0f, 150.0f),  0.001f);
        // Other border
        assertEquals(150.0f, FastMath.interpolateLinear(1.0f, 50.0f, 150.0f),  0.001f);
        // Way below the lower bound
        assertEquals(50.0f, FastMath.interpolateLinear(-100.0f, 50.0f, 150.0f),  0.001f);
        // Way above the higher bound
        assertEquals(150.0f, FastMath.interpolateLinear(200.0f, 50.0f, 150.0f), 0.001f);
    }

    @Test
    public void testVectorLinearInterpolate() {
        //Vectors to be tested
        Vector3f v1s = new Vector3f(10.0f, 30.0f, 25.0f);
        Vector3f v1e = new Vector3f(15.0f, 42.0f, 27.5f);
        Vector3f v1eneg = new Vector3f(-15.0f, -42.0f, -27.5f);

        //Resultvectors
        Vector3f v1r1 = new Vector3f(10.5f, 31.2f, 25.25f);
        Vector3f v1r2 = new Vector3f(12.5f, 36.0f, 26.25f);
        Vector3f v1r3 = new Vector3f(15.0f, 42.0f, 27.5f);
        Vector3f v1rneg = new Vector3f(-2.5f, -6.0f, -1.25f);

        //Test different scales with correct vectors
        assertEquals(v1r1, FastMath.interpolateLinear(0.1f, v1s, v1e));
        assertEquals(v1r2, FastMath.interpolateLinear(0.5f, v1s, v1e));
        assertEquals(v1r3, FastMath.interpolateLinear(1.0f, v1s, v1e));

        //Test negative end vector which is also smaller than the start vector
        assertEquals(v1rneg, FastMath.interpolateLinear(0.5f, v1s, v1eneg));
    }

    @Test
    public void interpolateCatmullRom() {
        float[] expected = new float[]{5.147461f, 5.3921876f, 5.7001953f, 6.0375f, 6.370117f, 6.6640625f, 6.8853517f};

        for (int i = 0; i < 7; i++) {
            float value = ((float) i + 1) / 8;

            assertEquals(expected[i], FastMath.interpolateCatmullRom(value, 0.1f, 0f, 5f, 7f, 9f), 0.001f);
        }

        assertEquals(5.0f, FastMath.interpolateCatmullRom(0.0f, 0.1f, 0f, 5f, 7f, 9f), 0.001f);
        assertEquals(7.0f, FastMath.interpolateCatmullRom(1.0f, 0.1f, 0f, 5f, 7f, 9f), 0.001f);
    }

    @Test
    public void interpolateBezier() {
        float[] expected = new float[]{1.7402344f, 3.234375f, 4.517578f, 5.625f, 6.591797f, 7.453125f, 8.244141f};

        for (int i = 0; i < 7; i++) {
            float value = ((float) i + 1) / 8;

            assertEquals(expected[i], FastMath.interpolateBezier(value, 0f, 5f, 7f, 9f), 0.001f);
        }

        assertEquals(0.0f, FastMath.interpolateBezier(0.0f, 0f, 5f, 7f, 9f), 0.001f);
        assertEquals(9.0f, FastMath.interpolateBezier(1.0f, 0f, 5f, 7f, 9f), 0.001f);
    }




}
