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

import android.opengl.Matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * Given data model.
 * The user is supposed to already have this.
 */
public class InfoVisModel {

    // Some points in model frame that the user is supposed to know to make the correspondence.
    private List<float[]> mModelPoints;

    public InfoVisModel() {
        mModelPoints = new ArrayList<float[]>();
        // Populate the points in model frame to make the correspondence. In this case they are
        // the four corners.
        mModelPoints.add(new float[]{0f, 1f, 0.01f, 1});
        mModelPoints.add(new float[]{0f, 0f, 0.01f, 1});
        mModelPoints.add(new float[]{1f, 0f, 0.01f, 1});
        mModelPoints.add(new float[]{1f, 1f, 0.01f, 1});
    }

    /**
     * Get the model points in OpenGl frame.
     */
    public List<float[]> getOpenGlModelPpoints(float[] openGlTHouse) {
        List<float[]> openGlPpoints = new ArrayList<float[]>();
        for (float[] modelPoint : mModelPoints) {
            float[] openGlPoint = new float[4];
            Matrix.multiplyMV(openGlPoint, 0, openGlTHouse, 0, modelPoint, 0);
            openGlPpoints.add(openGlPoint);
        }
        return openGlPpoints;
    }

    public int getNumberOfPoints() {
        return mModelPoints.size();
    }

}
