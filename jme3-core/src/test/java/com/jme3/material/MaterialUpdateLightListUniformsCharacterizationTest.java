package com.jme3.material;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.opengl.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.shader.Shader;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * Created by lennart on 04/04/16.
 */
public class MaterialUpdateLightListUniformsCharacterizationTest {

    @Test
    public void testUpdateLightListUniforms() {
        // We found this method in the detection of SOLID violations, what we want to do is refactor the switch/case
        //


        // We want to know how this method works, therefore we will try to list calls on mock objects and see how data
        // flows between the various parameters feeded to this material

        Material mat = new Material();

        Shader shader = new Shader(); // this cannot be mocked as it is final
        Geometry geom = mock(Geometry.class, withSettings().verboseLogging());


        RenderManager rm = mock(RenderManager.class, withSettings().verboseLogging());
        // We found that if you give a RenderManager with no Renderer, you'll get a NullPointer
        when(rm.getRenderer()).thenReturn(mock(Renderer.class));


        LightList ll = new LightList(mock(Spatial.class));// this cannot be mocked as it is final

        for (int i = 0; i < 5; i++) {
            // We want to test all branches of the switch, there are 4 light-types and a nonetype
            Light l = mock(Light.class);
            // We found that if a light doesnt have a colour, a nullpointer will be raised
            when(l.getColor()).thenReturn(new ColorRGBA(0,0,0,1.0f));
            // We also found that a light needs a Type
            // This is what we are after, we want to change these getType call and offload some of the code here to
            // a dedicated LightType class
            when(l.getType()).thenReturn(Light.Type.values()[i % Light.Type.values().length]);
            ll.add(l);

        }

        shader.initialize();

        mat.updateLightListUniforms(shader, geom, ll, 1, rm, 1);



    }
}
