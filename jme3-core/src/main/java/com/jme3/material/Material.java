/*
 * Copyright (c) 2009-2012 jMonkeyEngine
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
package com.jme3.material;

import com.jme3.asset.AssetKey;
import com.jme3.asset.AssetManager;
import com.jme3.asset.CloneableSmartAsset;
import com.jme3.export.*;
import com.jme3.light.*;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.material.TechniqueDef.LightMode;
import com.jme3.material.TechniqueDef.ShadowMode;
import com.jme3.math.*;
import com.jme3.renderer.Caps;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.Renderer;
import com.jme3.renderer.RendererException;
import com.jme3.renderer.geometryrenderers.GeometryRenderer;
import com.jme3.renderer.geometryrenderers.MultiPassGeometryRenderer;
import com.jme3.renderer.geometryrenderers.NoLightGeometryRenderer;
import com.jme3.renderer.geometryrenderers.SinglePassGeometryRenderer;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.instancing.InstancedGeometry;
import com.jme3.shader.Shader;
import com.jme3.shader.Uniform;
import com.jme3.shader.VarType;
import com.jme3.texture.Texture;
import com.jme3.texture.image.ColorSpace;
import com.jme3.util.ListMap;
import com.jme3.util.TempVars;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <code>Material</code> describes the rendering style for a given
 * {@link Geometry}.
 * <p>A material is essentially a list of {@link MatParam parameters},
 * those parameters map to uniforms which are defined in a shader.
 * Setting the parameters can modify the behavior of a
 * shader.
 * <p/>
 *
 * @author Kirill Vainer
 */
public class Material implements CloneableSmartAsset, Cloneable, Savable {

    // Version #2: Fixed issue with RenderState.apply*** flags not getting exported
    public static final int SAVABLE_VERSION = 2;
    private static final Logger logger = Logger.getLogger(Material.class.getName());
    private static final RenderState additiveLight = new RenderState();
    private static final RenderState depthOnly = new RenderState();
    private static final Quaternion nullDirLight = new Quaternion(0, -1, 0, -1);
    private static final boolean SUPERSHORTCUT = true;

    static {
        depthOnly.setDepthTest(true);
        depthOnly.setDepthWrite(true);
        depthOnly.setFaceCullMode(RenderState.FaceCullMode.Back);
        depthOnly.setColorWrite(false);

        additiveLight.setBlendMode(RenderState.BlendMode.AlphaAdditive);
        additiveLight.setDepthWrite(false);
    }
    private AssetKey key;
    private String name;
    private MaterialDef def;
    private ListMap<String, MatParam> paramValues = new ListMap<String, MatParam>();
    private Technique technique;
    private Map<String, Technique> techniques = new HashMap<String, Technique>();
    private int nextTexUnit = 0;
    private RenderState additionalState = null;
    private RenderState mergedRenderState = new RenderState();
    private boolean transparent = false;
    private boolean receivesShadows = false;
    private int sortingId = -1;
    private transient ColorRGBA ambientLightColor = new ColorRGBA(0, 0, 0, 1);

    public Material(MaterialDef def) {
        if (def == null) {
            throw new NullPointerException("Material definition cannot be null");
        }
        this.def = def;

        // Load default values from definition (if any)
        for (MatParam param : def.getMaterialParams()) {
            if (param.getValue() != null) {
                setParam(param.getName(), param.getVarType(), param.getValue());
            }
        }
    }

    public Material(AssetManager contentMan, String defName) {
        this((MaterialDef) contentMan.loadAsset(new AssetKey(defName)));
    }

    /**
     * Do not use this constructor. Serialization purposes only.
     */
    public Material() {
    }

    /**
     * Returns the asset key name of the asset from which this material was loaded.
     *
     * <p>This value will be <code>null</code> unless this material was loaded
     * from a .j3m file.
     *
     * @return Asset key name of the j3m file
     */
    public String getAssetName() {
        return key != null ? key.getName() : null;
    }

    /**
     * @return the name of the material (not the same as the asset name), the returned value can be null
     */
    public String getName() {
        return name;
    }

    /**
     * This method sets the name of the material.
     * The name is not the same as the asset name.
     * It can be null and there is no guarantee of its uniqueness.
     * @param name the name of the material
     */
    public void setName(String name) {
        this.name = name;
    }

    public void setKey(AssetKey key) {
        this.key = key;
    }

    public AssetKey getKey() {
        return key;
    }

    /**
     * Returns the sorting ID or sorting index for this material.
     *
     * <p>The sorting ID is used internally by the system to sort rendering
     * of geometries. It sorted to reduce shader switches, if the shaders
     * are equal, then it is sorted by textures.
     *
     * @return The sorting ID used for sorting geometries for rendering.
     */
    public int getSortId() {
        Technique t = getActiveTechnique();
        if (sortingId == -1 && t != null && t.getShader() != null) {
            int texId = -1;
            for (int i = 0; i < paramValues.size(); i++) {
                MatParam param = paramValues.getValue(i);
                if (param instanceof MatParamTexture) {
                    MatParamTexture tex = (MatParamTexture) param;
                    if (tex.getTextureValue() != null && tex.getTextureValue().getImage() != null) {
                        if (texId == -1) {
                            texId = 0;
                        }
                        texId += tex.getTextureValue().getImage().getId() % 0xff;
                    }
                }
            }
            sortingId = texId + t.getShader().getId() * 1000;
        }
        return sortingId;
    }

    /**
     * Clones this material. The result is returned.
     */
    @Override
    public Material clone() {
        try {
            Material mat = (Material) super.clone();

            if (additionalState != null) {
                mat.additionalState = additionalState.clone();
            }
            mat.technique = null;
            mat.techniques = new HashMap<String, Technique>();

            mat.paramValues = new ListMap<String, MatParam>();
            for (int i = 0; i < paramValues.size(); i++) {
                Map.Entry<String, MatParam> entry = paramValues.getEntry(i);
                mat.paramValues.put(entry.getKey(), entry.getValue().clone());
            }

            return mat;
        } catch (CloneNotSupportedException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * Compares two materials and returns true if they are equal.
     * This methods compare definition, parameters, additional render states.
     * Since materials are mutable objects, implementing equals() properly is not possible,
     * hence the name contentEquals().
     *
     * @param otherObj the material to compare to this material
     * @return true if the materials are equal.
     */
    public boolean contentEquals(Object otherObj) {
        if (!(otherObj instanceof Material)) {
            return false;
        }

        Material other = (Material) otherObj;

        // Early exit if the material are the same object
        if (this == other) {
            return true;
        }

        // Check material definition
        if (this.getMaterialDef() != other.getMaterialDef()) {
            return false;
        }

        // Early exit if the size of the params is different
        if (this.paramValues.size() != other.paramValues.size()) {
            return false;
        }

        // Checking technique
        if (this.technique != null || other.technique != null) {
            // Techniques are considered equal if their names are the same
            // E.g. if user chose custom technique for one material but
            // uses default technique for other material, the materials
            // are not equal.
            String thisDefName = this.technique != null ? this.technique.getDef().getName() : "Default";
            String otherDefName = other.technique != null ? other.technique.getDef().getName() : "Default";
            if (!thisDefName.equals(otherDefName)) {
                return false;
            }
        }

        // Comparing parameters
        for (String paramKey : paramValues.keySet()) {
            MatParam thisParam = this.getParam(paramKey);
            MatParam otherParam = other.getParam(paramKey);

            // This param does not exist in compared mat
            if (otherParam == null) {
                return false;
            }

            if (!otherParam.equals(thisParam)) {
                return false;
            }
        }

        // Comparing additional render states
        if (additionalState == null) {
            if (other.additionalState != null) {
                return false;
            }
        } else {
            if (!additionalState.equals(other.additionalState)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Works like {@link Object#hashCode() } except it may change together with the material as the material is mutable by definition.
     */
    public int contentHashCode() {
        int hash = 7;
        hash = 29 * hash + (this.def != null ? this.def.hashCode() : 0);
        hash = 29 * hash + (this.paramValues != null ? this.paramValues.hashCode() : 0);
        hash = 29 * hash + (this.technique != null ? this.technique.getDef().getName().hashCode() : 0);
        hash = 29 * hash + (this.additionalState != null ? this.additionalState.contentHashCode() : 0);
        return hash;
    }

    /**
     * Check if the transparent value marker is set on this material.
     * @return True if the transparent value marker is set on this material.
     * @see #setTransparent(boolean)
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * Set the transparent value marker.
     *
     * <p>This value is merely a marker, by itself it does nothing.
     * Generally model loaders will use this marker to indicate further
     * up that the material is transparent and therefore any geometries
     * using it should be put into the {@link Bucket#Transparent transparent
     * bucket}.
     *
     * @param transparent the transparent value marker.
     */
    public void setTransparent(boolean transparent) {
        this.transparent = transparent;
    }

    /**
     * Check if the material should receive shadows or not.
     *
     * @return True if the material should receive shadows.
     *
     * @see Material#setReceivesShadows(boolean)
     */
    public boolean isReceivesShadows() {
        return receivesShadows;
    }

    /**
     * Set if the material should receive shadows or not.
     *
     * <p>This value is merely a marker, by itself it does nothing.
     * Generally model loaders will use this marker to indicate
     * the material should receive shadows and therefore any
     * geometries using it should have the {@link ShadowMode#Receive} set
     * on them.
     *
     * @param receivesShadows if the material should receive shadows or not.
     */
    public void setReceivesShadows(boolean receivesShadows) {
        this.receivesShadows = receivesShadows;
    }

    /**
     * Get the material definition (j3md file info) that <code>this</code>
     * material is implementing.
     *
     * @return the material definition this material implements.
     */
    public MaterialDef getMaterialDef() {
        return def;
    }

    /**
     * Returns the parameter set on this material with the given name,
     * returns <code>null</code> if the parameter is not set.
     *
     * @param name The parameter name to look up.
     * @return The MatParam if set, or null if not set.
     */
    public MatParam getParam(String name) {
        return paramValues.get(name);
    }

    /**
     * Returns the texture parameter set on this material with the given name,
     * returns <code>null</code> if the parameter is not set.
     *
     * @param name The parameter name to look up.
     * @return The MatParamTexture if set, or null if not set.
     */
    public MatParamTexture getTextureParam(String name) {
        MatParam param = paramValues.get(name);
        if (param instanceof MatParamTexture) {
            return (MatParamTexture) param;
        }
        return null;
    }

    /**
     * Returns a collection of all parameters set on this material.
     *
     * @return a collection of all parameters set on this material.
     *
     * @see #setParam(java.lang.String, com.jme3.shader.VarType, java.lang.Object)
     */
    public Collection<MatParam> getParams() {
        return paramValues.values();
    }

    /**
     * Returns the ListMap of all parameters set on this material.
     *
     * @return a ListMap of all parameters set on this material.
     *
     * @see #setParam(java.lang.String, com.jme3.shader.VarType, java.lang.Object)
     */
    public ListMap getParamsMap() {
        return paramValues;
    }

    /**
     * Check if setting the parameter given the type and name is allowed.
     * @param type The type that the "set" function is designed to set
     * @param name The name of the parameter
     */
    private void checkSetParam(VarType type, String name) {
        MatParam paramDef = def.getMaterialParam(name);
        if (paramDef == null) {
            throw new IllegalArgumentException("Material parameter is not defined: " + name);
        }
        if (type != null && paramDef.getVarType() != type) {
            logger.log(Level.WARNING, "Material parameter being set: {0} with "
                    + "type {1} doesn''t match definition types {2}", new Object[]{name, type.name(), paramDef.getVarType()});
        }
    }

    /**
     * Pass a parameter to the material shader.
     *
     * @param name the name of the parameter defined in the material definition (j3md)
     * @param type the type of the parameter {@link VarType}
     * @param value the value of the parameter
     */
    public void setParam(String name, VarType type, Object value) {
        checkSetParam(type, name);

        if (type.isTextureType()) {
            setTextureParam(name, type, (Texture)value);
        } else {
            MatParam val = getParam(name);
            if (val == null) {
                MatParam paramDef = def.getMaterialParam(name);
                paramValues.put(name, new MatParam(type, name, value));
            } else {
                val.setValue(value);
            }

            if (technique != null) {
                technique.notifyParamChanged(name, type, value);
            }
        }
    }

    /**
     * Clear a parameter from this material. The parameter must exist
     * @param name the name of the parameter to clear
     */
    public void clearParam(String name) {
        checkSetParam(null, name);
        MatParam matParam = getParam(name);
        if (matParam == null) {
            return;
        }

        paramValues.remove(name);
        if (matParam instanceof MatParamTexture) {
            int texUnit = ((MatParamTexture) matParam).getUnit();
            nextTexUnit--;
            for (MatParam param : paramValues.values()) {
                if (param instanceof MatParamTexture) {
                    MatParamTexture texParam = (MatParamTexture) param;
                    if (texParam.getUnit() > texUnit) {
                        texParam.setUnit(texParam.getUnit() - 1);
                    }
                }
            }
            sortingId = -1;
        }
        if (technique != null) {
            technique.notifyParamChanged(name, null, null);
        }
    }

    /**
     * Set a texture parameter.
     *
     * @param name The name of the parameter
     * @param type The variable type {@link VarType}
     * @param value The texture value of the parameter.
     *
     * @throws IllegalArgumentException is value is null
     */
    public void setTextureParam(String name, VarType type, Texture value) {
        if (value == null) {
            throw new IllegalArgumentException();
        }

        checkSetParam(type, name);
        MatParamTexture val = getTextureParam(name);
        if (val == null) {
            MatParamTexture paramDef = (MatParamTexture) def.getMaterialParam(name);
            if (paramDef.getColorSpace() != null && paramDef.getColorSpace() != value.getImage().getColorSpace()) {
                value.getImage().setColorSpace(paramDef.getColorSpace());
                logger.log(Level.FINE, "Material parameter {0} needs a {1} texture, "
                        + "texture {2} was switched to {3} color space.",
                        new Object[]{name, paramDef.getColorSpace().toString(),
                            value.getName(),
                            value.getImage().getColorSpace().name()});
            } else if (paramDef.getColorSpace() == null && value.getName() != null && value.getImage().getColorSpace() == ColorSpace.Linear) {
                logger.log(Level.WARNING,
                        "The texture {0} has linear color space, but the material "
                        + "parameter {2} specifies no color space requirement, this may "
                        + "lead to unexpected behavior.\nCheck if the image "
                        + "was not set to another material parameter with a linear "
                        + "color space, or that you did not set the ColorSpace to "
                        + "Linear using texture.getImage.setColorSpace().",
                        new Object[]{value.getName(), value.getImage().getColorSpace().name(), name});
            }
            paramValues.put(name, new MatParamTexture(type, name, value, nextTexUnit++, null));
        } else {
            val.setTextureValue(value);
        }

        if (technique != null) {
            technique.notifyParamChanged(name, type, nextTexUnit - 1);
        }

        // need to recompute sort ID
        sortingId = -1;
    }

    /**
     * Pass a texture to the material shader.
     *
     * @param name the name of the texture defined in the material definition
     * (j3md) (for example Texture for Lighting.j3md)
     * @param value the Texture object previously loaded by the asset manager
     */
    public void setTexture(String name, Texture value) {
        if (value == null) {
            // clear it
            clearParam(name);
            return;
        }

        VarType paramType = null;
        switch (value.getType()) {
            case TwoDimensional:
                paramType = VarType.Texture2D;
                break;
            case TwoDimensionalArray:
                paramType = VarType.TextureArray;
                break;
            case ThreeDimensional:
                paramType = VarType.Texture3D;
                break;
            case CubeMap:
                paramType = VarType.TextureCubeMap;
                break;
            default:
                throw new UnsupportedOperationException("Unknown texture type: " + value.getType());
        }

        setTextureParam(name, paramType, value);
    }

    /**
     * Pass a Matrix4f to the material shader.
     *
     * @param name the name of the matrix defined in the material definition (j3md)
     * @param value the Matrix4f object
     */
    public void setMatrix4(String name, Matrix4f value) {
        setParam(name, VarType.Matrix4, value);
    }

    /**
     * Pass a boolean to the material shader.
     *
     * @param name the name of the boolean defined in the material definition (j3md)
     * @param value the boolean value
     */
    public void setBoolean(String name, boolean value) {
        setParam(name, VarType.Boolean, value);
    }

    /**
     * Pass a float to the material shader.
     *
     * @param name the name of the float defined in the material definition (j3md)
     * @param value the float value
     */
    public void setFloat(String name, float value) {
        setParam(name, VarType.Float, value);
    }

    /**
     * Pass a float to the material shader.  This version avoids auto-boxing
     * if the value is already a Float.
     *
     * @param name the name of the float defined in the material definition (j3md)
     * @param value the float value
     */
    public void setFloat(String name, Float value) {
        setParam(name, VarType.Float, value);
    }

    /**
     * Pass an int to the material shader.
     *
     * @param name the name of the int defined in the material definition (j3md)
     * @param value the int value
     */
    public void setInt(String name, int value) {
        setParam(name, VarType.Int, value);
    }

    /**
     * Pass a Color to the material shader.
     *
     * @param name the name of the color defined in the material definition (j3md)
     * @param value the ColorRGBA value
     */
    public void setColor(String name, ColorRGBA value) {
        setParam(name, VarType.Vector4, value);
    }

    /**
     * Pass a Vector2f to the material shader.
     *
     * @param name the name of the Vector2f defined in the material definition (j3md)
     * @param value the Vector2f value
     */
    public void setVector2(String name, Vector2f value) {
        setParam(name, VarType.Vector2, value);
    }

    /**
     * Pass a Vector3f to the material shader.
     *
     * @param name the name of the Vector3f defined in the material definition (j3md)
     * @param value the Vector3f value
     */
    public void setVector3(String name, Vector3f value) {
        setParam(name, VarType.Vector3, value);
    }

    /**
     * Pass a Vector4f to the material shader.
     *
     * @param name the name of the Vector4f defined in the material definition (j3md)
     * @param value the Vector4f value
     */
    public void setVector4(String name, Vector4f value) {
        setParam(name, VarType.Vector4, value);
    }


    public void write(JmeExporter ex) throws IOException {
        OutputCapsule oc = ex.getCapsule(this);
        oc.write(def.getAssetName(), "material_def", null);
        oc.write(additionalState, "render_state", null);
        oc.write(transparent, "is_transparent", false);
        oc.write(name, "name", null);
        oc.writeStringSavableMap(paramValues, "parameters", null);
    }

    public void read(JmeImporter im) throws IOException {
        InputCapsule ic = im.getCapsule(this);

        name = ic.readString("name", null);
        additionalState = (RenderState) ic.readSavable("render_state", null);
        transparent = ic.readBoolean("is_transparent", false);

        // Load the material def
        String defName = ic.readString("material_def", null);
        HashMap<String, MatParam> params = (HashMap<String, MatParam>) ic.readStringSavableMap("parameters", null);

        boolean enableVcolor = false;
        boolean separateTexCoord = false;
        boolean applyDefaultValues = false;
        boolean guessRenderStateApply = false;

        int ver = ic.getSavableVersion(Material.class);
        if (ver < 1) {
            applyDefaultValues = true;
        }
        if (ver < 2) {
            guessRenderStateApply = true;
        }
        if (im.getFormatVersion() == 0) {
            // Enable compatibility with old models
            if (defName.equalsIgnoreCase("Common/MatDefs/Misc/VertexColor.j3md")) {
                // Using VertexColor, switch to Unshaded and set VertexColor=true
                enableVcolor = true;
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/SimpleTextured.j3md")
                    || defName.equalsIgnoreCase("Common/MatDefs/Misc/SolidColor.j3md")) {
                // Using SimpleTextured/SolidColor, just switch to Unshaded
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/WireColor.j3md")) {
                // Using WireColor, set wireframe renderstate = true and use Unshaded
                getAdditionalRenderState().setWireframe(true);
                defName = "Common/MatDefs/Misc/Unshaded.j3md";
            } else if (defName.equalsIgnoreCase("Common/MatDefs/Misc/Unshaded.j3md")) {
                // Uses unshaded, ensure that the proper param is set
                MatParam value = params.get("SeperateTexCoord");
                if (value != null && ((Boolean) value.getValue()) == true) {
                    params.remove("SeperateTexCoord");
                    separateTexCoord = true;
                }
            }
            assert applyDefaultValues && guessRenderStateApply;
        }

        def = (MaterialDef) im.getAssetManager().loadAsset(new AssetKey(defName));
        paramValues = new ListMap<String, MatParam>();

        // load the textures and update nextTexUnit
        for (Map.Entry<String, MatParam> entry : params.entrySet()) {
            MatParam param = entry.getValue();
            if (param instanceof MatParamTexture) {
                MatParamTexture texVal = (MatParamTexture) param;

                if (nextTexUnit < texVal.getUnit() + 1) {
                    nextTexUnit = texVal.getUnit() + 1;
                }

                // the texture failed to load for this param
                // do not add to param values
                if (texVal.getTextureValue() == null || texVal.getTextureValue().getImage() == null) {
                    continue;
                }
            }

            if (im.getFormatVersion() == 0 && param.getName().startsWith("m_")) {
                // Ancient version of jME3 ...
                param.setName(param.getName().substring(2));
            }

            if (def.getMaterialParam(param.getName()) == null) {
                logger.log(Level.WARNING, "The material parameter is not defined: {0}. Ignoring..",
                        param.getName());
            } else {
                checkSetParam(param.getVarType(), param.getName());
                paramValues.put(param.getName(), param);
            }
        }

        if (applyDefaultValues) {
            // compatability with old versions where default vars were
            // not available
            for (MatParam param : def.getMaterialParams()) {
                if (param.getValue() != null && paramValues.get(param.getName()) == null) {
                    setParam(param.getName(), param.getVarType(), param.getValue());
                }
            }
        }
        if (guessRenderStateApply && additionalState != null) {
            // Try to guess values of "apply" render state based on defaults
            // if value != default then set apply to true
            additionalState.applyPolyOffset = additionalState.offsetEnabled;
            additionalState.applyAlphaFallOff = additionalState.alphaTest;
            additionalState.applyAlphaTest = additionalState.alphaTest;
            additionalState.applyBlendMode = additionalState.blendMode != BlendMode.Off;
            additionalState.applyColorWrite = !additionalState.colorWrite;
            additionalState.applyCullMode = additionalState.cullMode != FaceCullMode.Back;
            additionalState.applyDepthTest = !additionalState.depthTest;
            additionalState.applyDepthWrite = !additionalState.depthWrite;
            additionalState.applyPointSprite = additionalState.pointSprite;
            additionalState.applyStencilTest = additionalState.stencilTest;
            additionalState.applyWireFrame = additionalState.wireframe;
        }
        if (enableVcolor) {
            setBoolean("VertexColor", true);
        }
        if (separateTexCoord) {
            setBoolean("SeparateTexCoord", true);
        }
    }

    /**
     * Acquire the additional {@link RenderState render state} to apply
     * for this material.
     *
     * <p>The first call to this method will create an additional render
     * state which can be modified by the user to apply any render
     * states in addition to the ones used by the renderer. Only render
     * states which are modified in the additional render state will be applied.
     *
     * @return The additional render state.
     */
    public RenderState getAdditionalRenderState() {
        if (additionalState == null) {
            additionalState = RenderState.ADDITIONAL.clone();
        }
        return additionalState;
    }

    public Map<String, Technique> getTechiques() {
        return this.techniques;
    }

    /**
     * Returns the currently active technique.
     * <p>
     * The technique is selected automatically by the {@link RenderManager}
     * based on system capabilities. Users may select their own
     * technique by using
     * {@link #selectTechnique(java.lang.String, com.jme3.renderer.RenderManager) }.
     *
     * @return the currently active technique.
     *
     * @see #selectTechnique(java.lang.String, com.jme3.renderer.RenderManager)
     */
    public Technique getActiveTechnique() {
        return technique;
    }

    /**
     * Sets the technique this material will be rendered with
     *
     * Should only be used by the Geometry, for selecting what technique is used on a Geometry see
     * {@link Geometry#selectTechnique(String, RenderManager)}. This method is for storage inside this
     * data object only!
     *
     * @param tech
     */
    public void setActiveTechnique(Technique tech) {
        this.technique = tech;
    }
}
