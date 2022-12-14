/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.functionally.infovis;

import com.google.atap.tangoservice.TangoPoseData;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;

import org.rajawali3d.Object3D;
import org.rajawali3d.lights.DirectionalLight;
import org.rajawali3d.materials.Material;
import org.rajawali3d.materials.methods.DiffuseMethod;
import org.rajawali3d.materials.methods.SpecularMethod;
import org.rajawali3d.materials.textures.ATexture;
import org.rajawali3d.materials.textures.StreamingTexture;
import org.rajawali3d.math.Matrix4;
import org.rajawali3d.math.Quaternion;
import org.rajawali3d.math.vector.Vector3;
import org.rajawali3d.primitives.Cube;
import org.rajawali3d.primitives.Line3D;
import org.rajawali3d.primitives.ScreenQuad;
import org.rajawali3d.primitives.Sphere;
import org.rajawali3d.renderer.Renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;

/**
 * Simple example augmented reality renderer which displays spheres fixed in place for every
 * point measurement and a 3D model of a info in the position given by the found correspondence.
 * Whenever the user clicks on '+' button, a sphere is placed in the aimed position with the
 * crosshair.
 */
public class InfoVisRenderer extends Renderer {
    private static final float SPHERE_RADIUS = 0.01f;
    private static final String TAG = InfoVisRenderer.class.getSimpleName();

    private float[] textureCoords0 = new float[]{0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 0.0F};
    private float[] textureCoords270 = new float[]{1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F};
    private float[] textureCoords180 = new float[]{1.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F};
    private float[] textureCoords90 = new float[]{0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F};

    // Augmented reality related fields
    private ATexture mTangoCameraTexture;
    private boolean mSceneCameraConfigured;

    private Object3D mInfoObject3D;
    private Object3D mNextPointObject3D;
    private List<Object3D> mDestPointsObjectList = new ArrayList<Object3D>();
    private Material mSphereMaterial;
    private Material mInfoMaterial;

    private ScreenQuad mBackgroundQuad;

    public InfoVisRenderer(Context context) {
        super(context);
    }

    @Override
    protected void initScene() {
        // Create a quad covering the whole background and assign a texture to it where the
        // Tango color camera contents will be rendered.
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
            mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0);
        }
        Material tangoCameraMaterial = new Material();
        tangoCameraMaterial.setColorInfluence(0);
        // We need to use Rajawali's {@code StreamingTexture} since it sets up the texture
        // for GL_TEXTURE_EXTERNAL_OES rendering
        mTangoCameraTexture =
                new StreamingTexture("camera", (StreamingTexture.ISurfaceListener) null);
        try {
            tangoCameraMaterial.addTexture(mTangoCameraTexture);
            mBackgroundQuad.setMaterial(tangoCameraMaterial);
        } catch (ATexture.TextureException e) {
            Log.e(TAG, "Exception creating texture for RGB camera contents", e);
        }
        getCurrentScene().addChildAt(mBackgroundQuad, 0);

        // Add two directional lights in arbitrary directions.
        DirectionalLight light = new DirectionalLight(1, 0.2, -1);
        light.setColor(1, 1, 1);
        light.setPower(0.8f);
        light.setPosition(0.3, 0.2, 0.4);
        getCurrentScene().addLight(light);

        DirectionalLight light2 = new DirectionalLight(-1, 0.2, -1);
        light.setColor(1, 4, 4);
        light.setPower(0.8f);
        light.setPosition(0.3, 0.3, 0.3);
        getCurrentScene().addLight(light2);

        // Set-up a materials.
        mSphereMaterial = new Material();
        mSphereMaterial.enableLighting(true);
        mSphereMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mSphereMaterial.setSpecularMethod(new SpecularMethod.Phong());

        mInfoMaterial = new Material();
        mInfoMaterial.enableLighting(true);
        mInfoMaterial.setDiffuseMethod(new DiffuseMethod.Lambert());
        mInfoMaterial.setSpecularMethod(new SpecularMethod.Phong());

        mInfoObject3D = new Object3D();
        Material lineMaterial = new Material();
        int n = 1;
        float delta = 1f / n;
        for (int x = 0; x <= n; ++x)
            for (int y = 0; y <= n; ++y) {
                Stack<Vector3> points = new Stack();
                points.push(new Vector3(x * delta, y * delta, 0));
                points.push(new Vector3(x * delta, y * delta, 1));
                Object3D line = new Line3D(points, 1f, x == 0 && y == 0 ? Color.RED : Color.WHITE);
                line.setMaterial(lineMaterial);
                mInfoObject3D.addChild(line);
            }
        for (int y = 0; y <= n; ++y)
            for (int z = 0; z <= n; ++z) {
                Stack<Vector3> points = new Stack();
                points.push(new Vector3(0, y * delta, z * delta));
                points.push(new Vector3(1, y * delta, z * delta));
                Object3D line = new Line3D(points, 1f, y == 0 && z == 0 ? Color.RED : Color.WHITE);
                line.setMaterial(lineMaterial);
                mInfoObject3D.addChild(line);
            }
        for (int z = 0; z <= n; ++z)
            for (int x = 0; x <= n; ++x) {
                Stack<Vector3> points = new Stack();
                points.push(new Vector3(x * delta, 0, z * delta));
                points.push(new Vector3(x * delta, 1, z * delta));
                Object3D line = new Line3D(points, 1f, z == 0 && x == 0 ? Color.RED : Color.WHITE);
                line.setMaterial(lineMaterial);
                mInfoObject3D.addChild(line);
            }
        n = 18;
        delta = 1f / n;
        for (int ix = 0; ix <= n; ++ix)
            for (int iy = 0; iy <= n; ++iy) {
                double x = Math.min(ix * delta                    * (1 + (Math.random() - 0.5) / 3), 1);
                double y = Math.min(iy * delta                    * (1 + (Math.random() - 0.5) / 3), 1);
                double z = Math.min(Math.sin(x * Math.PI / 2) * y * (1 + (Math.random() - 0.5) / 3), 1);
                Object3D box = new Cube(0.025f * (float) Math.random());
                box.setMaterial(mInfoMaterial);
                box.setColor(0xff000000 | (int) (0x00ffffff * Math.random()));
                box.setPosition((float) x, (float) y, (float) z);
                mInfoObject3D.addChild(box);
                }
        mInfoObject3D.setMaterial(mInfoMaterial);
        getCurrentScene().addChild(mInfoObject3D);

    }

    /**
     * Update background texture's UV coordinates when device orientation is changed. i.e change
     * between landscape and portrait mode.
     * This must be run in the OpenGL thread.
     */
    public void updateColorCameraTextureUvGlThread(int rotation) {
        if (mBackgroundQuad == null) {
            mBackgroundQuad = new ScreenQuad();
        }

        switch (rotation) {
            case Surface.ROTATION_90:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords90, true);
                break;
            case Surface.ROTATION_180:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords180, true);
                break;
            case Surface.ROTATION_270:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords270, true);
                break;
            default:
                mBackgroundQuad.getGeometry().setTextureCoords(textureCoords0, true);
                break;
        }
        mBackgroundQuad.getGeometry().reload();
    }

    /**
     * It returns the ID currently assigned to the texture where the Tango color camera contents
     * should be rendered.
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public int getTextureId() {
        return mTangoCameraTexture == null ? -1 : mTangoCameraTexture.getTextureId();
    }

    /**
     * We need to override this method to mark the camera for re-configuration (set proper
     * projection matrix) since it will be reset by Rajawali on surface changes.
     */
    @Override
    public void onRenderSurfaceSizeChanged(GL10 gl, int width, int height) {
        super.onRenderSurfaceSizeChanged(gl, width, height);
        mSceneCameraConfigured = false;
    }

    public boolean isSceneCameraConfigured() {
        return mSceneCameraConfigured;
    }

    /**
     * Sets the projection matrix for the scene camera to match the parameters of the color camera,
     * provided by the {@code TangoCameraIntrinsics}.
     */
    public void setProjectionMatrix(float[] matrixFloats) {
        getCurrentCamera().setProjectionMatrix(new Matrix4(matrixFloats));
    }

    /**
     * Render the data model.
     * Render the next correspondence source point to be added as a green sphere. Render the
     * correspondence destination points as red spheres. Render the 3d model in the position and
     * orientation given by the found correspondence transform.
     */
    public void updateModelRendering(InfoVisModel infoVisModel, float[] openGlTInfo,
                                     List<float[]> destPoints) {
        if (destPoints.size() > mDestPointsObjectList.size()) {
            // If new destination points were measured, then add them as points as red spheres.
            for (int i = mDestPointsObjectList.size(); i < destPoints.size(); i++) {
                Object3D destPointObject3D = makePoint(destPoints.get(i), Color.RED);
                getCurrentScene().addChild(destPointObject3D);
                mDestPointsObjectList.add(destPointObject3D);
            }
        } else if (destPoints.size() < mDestPointsObjectList.size()) {
            // If destination points were deleted, then add them as points as red spheres.
            for (int i = destPoints.size(); i < mDestPointsObjectList.size(); i++) {
                Object3D destPointObject3D = mDestPointsObjectList.get(i);
                getCurrentScene().removeChild(destPointObject3D);
                mDestPointsObjectList.remove(i);
            }
        }

        // Move the position of the next source point to be added.
        int nextPointNumber = destPoints.size();
        List<float[]> infoModelPoints = infoVisModel.getOpenGlModelPpoints(openGlTInfo);
        if (nextPointNumber < infoModelPoints.size()) {
            if (mNextPointObject3D == null) {
                mNextPointObject3D = makePoint(new float[]{0, 0, 0}, Color.GREEN);
                getCurrentScene().addChild(mNextPointObject3D);
            }
            float[] position = infoModelPoints.get(nextPointNumber);
            mNextPointObject3D.setPosition(position[0], position[1], position[2]);
        } else {
            getCurrentScene().removeChild(mNextPointObject3D);
            mNextPointObject3D = null;
        }

        // Place the info object in the position and orientation given by the correspondence
        // transform.
        if (mInfoObject3D != null) {
            Matrix4 transform = new Matrix4(openGlTInfo);
            double scale = transform.getScaling().x;
            mInfoObject3D.setScale(scale);
            // Multiply by the inverse of the scale so the transform is only rotation and
            // translation.
            Vector3 translation = transform.getTranslation();
            Matrix4 invScale = Matrix4.createScaleMatrix(1 / scale, 1 / scale, 1 / scale);
            transform.multiply(invScale);
            Quaternion orientation = new Quaternion().fromMatrix(transform);
            orientation.normalize();
            mInfoObject3D.setPosition(translation);
            mInfoObject3D.setOrientation(orientation);
        }
    }


    /**
     * Update the scene camera based on the provided pose in Tango start of service frame.
     * The camera pose should match the pose of the camera color at the time the last rendered RGB
     * frame, which can be retrieved with this.getTimestamp();
     * <p/>
     * NOTE: This must be called from the OpenGL render thread - it is not thread safe.
     */
    public void updateRenderCameraPose(TangoPoseData cameraPose) {
        float[] rotation = cameraPose.getRotationAsFloats();
        float[] translation = cameraPose.getTranslationAsFloats();
        Quaternion quaternion = new Quaternion(rotation[3], rotation[0], rotation[1], rotation[2]);
        // Conjugating the Quaternion is need because Rajawali uses left handed convention for
        // quaternions.
        getCurrentCamera().setRotation(quaternion.conjugate());
        getCurrentCamera().setPosition(translation[0], translation[1], translation[2]);
    }

    @Override
    public void onOffsetsChanged(float xOffset, float yOffset,
                                 float xOffsetStep, float yOffsetStep,
                                 int xPixelOffset, int yPixelOffset) {
    }

    @Override
    public void onTouchEvent(MotionEvent event) {

    }

    /**
     * Render the new correspondence destination point measurements as red spheres.
     */
    private Object3D makePoint(float[] openGLPpoint, int color) {
        Object3D object3D = new Sphere(SPHERE_RADIUS, 10, 10);
        object3D.setMaterial(mSphereMaterial);
        object3D.setColor(color);
        object3D.setPosition(openGLPpoint[0], openGLPpoint[1], openGLPpoint[2]);
        return object3D;
    }

}
