/*
 * Copyright 2022 The Android Open Source Project
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
package androidx.camera.core;

import static androidx.core.util.Preconditions.checkArgument;

import android.os.Build;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;

/**
 * An effect for one or multiple camera outputs.
 *
 * <p>A {@link CameraEffect} class contains 2 types of information, the processor and the
 * configuration.
 * <ul>
 * <li> The processor is an implementation of either {@link SurfaceProcessor} or
 * {@link ImageProcessor}. It consumes original camera frames from CameraX, applies the effect,
 * and returns the processed frames back to CameraX.
 * <li> The configuration provides information on how the processor should be injected into the
 * pipeline. For example, the target {@link UseCase}s where the effect should be applied.
 * </ul>
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraEffect {

    /**
     * Bitmask options for the effect targets.
     *
     * @hide
     */
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef(flag = true, value = {PREVIEW, VIDEO_CAPTURE, IMAGE_CAPTURE})
    public @interface Targets {
    }

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@link Preview}.
     */
    public static final int PREVIEW = 1;

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@code VideoCapture}.
     */
    public static final int VIDEO_CAPTURE = 1 << 1;

    /**
     * Bitmask option to indicate that CameraX should apply this effect to {@link ImageCapture}.
     */
    public static final int IMAGE_CAPTURE = 1 << 2;

    @Targets
    private final int mTargets;
    @NonNull
    private final Executor mExecutor;
    @Nullable
    private final SurfaceProcessor mSurfaceProcessor;
    @Nullable
    private final ImageProcessor mImageProcessor;

    /**
     * @param targets        the target {@link UseCase} to which this effect should be applied.
     *                       Currently, {@link ImageProcessor} can only target
     *                       {@link #IMAGE_CAPTURE}. Targeting other {@link UseCase} will throw
     *                       {@link IllegalArgumentException}.
     * @param executor       the {@link Executor} on which the {@param imageProcessor} will be
     *                       invoked.
     * @param imageProcessor a {@link ImageProcessor} implementation. Once the effect is active,
     *                       CameraX will send frames to the {@link ImageProcessor} on the
     *                       {@param executor}, and deliver the processed frames to the app.
     */
    protected CameraEffect(
            @Targets int targets,
            @NonNull Executor executor,
            @NonNull ImageProcessor imageProcessor) {
        checkArgument(targets == IMAGE_CAPTURE,
                "Currently ImageProcessor can only target IMAGE_CAPTURE.");
        mTargets = targets;
        mExecutor = executor;
        mSurfaceProcessor = null;
        mImageProcessor = imageProcessor;
    }

    /**
     * @param targets          the target {@link UseCase} to which this effect should be applied.
     *                         Currently {@link SurfaceProcessor} can only target {@link #PREVIEW}.
     *                         Targeting other {@link UseCase} will throw
     *                         {@link IllegalArgumentException}.
     * @param executor         the {@link Executor} on which the {@param imageProcessor} will be
     *                         invoked.
     * @param surfaceProcessor a {@link SurfaceProcessor} implementation. Once the effect is
     *                         active, CameraX will send frames to the {@link SurfaceProcessor}
     *                         on the {@param executor}, and deliver the processed frames to the
     *                         app.
     */
    protected CameraEffect(
            @Targets int targets,
            @NonNull Executor executor,
            @NonNull SurfaceProcessor surfaceProcessor) {
        checkArgument(targets == PREVIEW,
                "Currently SurfaceProcessor can only target PREVIEW.");
        mTargets = targets;
        mExecutor = executor;
        mSurfaceProcessor = surfaceProcessor;
        mImageProcessor = null;
    }

    /**
     * Ges the target {@link UseCase}s of this effect.
     */
    @Targets
    public int getTargets() {
        return mTargets;
    }

    /**
     * Gets the {@link Executor} associated with this effect.
     *
     * <p>This method returns the value set in {@link CameraEffect}'s constructor.
     */
    @NonNull
    public Executor getExecutor() {
        return mExecutor;
    }

    /**
     * Gets the {@link SurfaceProcessor} associated with this effect.
     */
    @Nullable
    public SurfaceProcessor getSurfaceProcessor() {
        return mSurfaceProcessor;
    }

    /**
     * Gets the {@link ImageProcessor} associated with this effect.
     */
    @Nullable
    public ImageProcessor getImageProcessor() {
        return mImageProcessor;
    }
}
