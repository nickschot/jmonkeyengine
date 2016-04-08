package com.jme3.renderer.geometryrenderers;

import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

/**
 * Created by Lennart on 08/04/2016.
 */
public class NoLightGeometryRenderer extends GeometryRenderer {
    public NoLightGeometryRenderer(Geometry g, RenderManager rm) {
        super(g, rm);
    }

    @Override
    public void render() {

    }
}
