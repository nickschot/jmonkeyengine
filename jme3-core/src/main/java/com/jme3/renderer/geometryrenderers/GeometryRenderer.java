package com.jme3.renderer.geometryrenderers;

import com.jme3.light.LightList;
import com.jme3.material.*;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.util.ListMap;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * A class that contains all information to render a Geometry
 */
public abstract class GeometryRenderer {
    protected final Geometry geometry;
    protected final RenderManager renderManager;

    protected RenderState mergedRenderState = new RenderState();

    public GeometryRenderer(Geometry g, RenderManager rm) {
        this.geometry = g;
        this.renderManager = rm;
    }

    public void render() {
        Technique technique = this.geometry.getMaterial().getActiveTechnique();
        Renderer renderer = this.renderManager.getRenderer();

        RenderState rs = this.geometry.getMaterial().getAdditionalRenderState();

        Shader shader = technique.getShader();

        if (renderManager.getForcedRenderState() != null) {
            renderer.applyRenderState(renderManager.getForcedRenderState());
        } else {
            TechniqueDef techDef = technique.getDef();

            if (techDef.getRenderState() != null) {
                renderer.applyRenderState(techDef.getRenderState().copyMergedTo(rs, mergedRenderState));
            } else {
                renderer.applyRenderState(RenderState.DEFAULT.copyMergedTo(rs, mergedRenderState));
            }
        }


        clearUniformsSetByCurrent(shader);
        renderManager.updateUniformBindings(this.geometry.getMaterial().getActiveTechnique().getWorldBindUniforms());

        ListMap<String, MatParam> paramsMap = geometry.getMaterial().getParamsMap();

        for (int i = 0; i < paramsMap.size(); i++) {
            MatParam param = paramsMap.getValue(i);
            param.apply(renderer, technique);
        }

        this.renderForLighting();
    }

    public abstract void renderForLighting();

    protected void renderMeshFromGeometry() {
        Mesh mesh = this.geometry.getMesh();
        int lodLevel = this.geometry.getLodLevel();

        Renderer renderer = this.renderManager.getRenderer();


        if (this.geometry instanceof InstancedGeometry) {
            InstancedGeometry instGeom = (InstancedGeometry) this.geometry;
            int numInstances = instGeom.getActualNumInstances();
            if (numInstances == 0) {
                return;
            }
            if (this.renderManager.getRenderer().getCaps().contains(Caps.MeshInstancing)) {
                renderer.renderMesh(mesh, lodLevel, numInstances, instGeom.getAllInstanceData());
            } else {
                throw new RendererException("Mesh instancing is not supported by the video hardware");
            }
        } else {
            renderer.renderMesh(mesh, lodLevel, 1, null);
        }
    }

    protected void resetUniformsNotSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            if (!u.isSetByCurrentMaterial()) {
                if (u.getName().charAt(0) != 'g') {
                    // Don't reset world globals!
                    // The benefits gained from this are very minimal
                    // and cause lots of matrix -> FloatBuffer conversions.
                    u.clearValue();
                }
            }
        }
    }

    protected void clearUniformsSetByCurrent(Shader shader) {
        ListMap<String, Uniform> uniforms = shader.getUniformMap();
        int size = uniforms.size();
        for (int i = 0; i < size; i++) {
            Uniform u = uniforms.getValue(i);
            u.clearSetByCurrentMaterial();
        }
    }

}