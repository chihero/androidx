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

package androidx.graphics.lowlatency

import android.app.UiAutomation
import android.graphics.Color
import android.hardware.HardwareBuffer
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Build
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.surface.SurfaceControlCompat
import androidx.graphics.surface.SurfaceControlUtils
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class GLFrontBufferedRendererTest {

    companion object {
        val TAG = "GLFrontBufferedRenderer"
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testFrontBufferedLayerRender() {
        val renderLatch = CountDownLatch(1)
        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {

            val mProjectionMatrix = FloatArray(16)
            val mOrthoMatrix = FloatArray(16)

            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED ==
                    bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testDoubleBufferedLayerRender() {
        val renderLatch = CountDownLatch(1)
        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {

            private val mOrthoMatrix = FloatArray(16)
            private val mProjectionMatrix = FloatArray(16)

            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
            }

            override fun onDoubleBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
                renderer?.commit()
            }
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (Math.abs(
                    Color.red(Color.BLUE) - Color.red(
                        bitmap.getPixel(
                            coords[0] + width / 2,
                            coords[1] + height / 2
                        )
                    )
                ) < 2) &&
                    (Math.abs(
                        Color.green(Color.BLUE) - Color.green(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2) &&
                    (Math.abs(
                        Color.blue(Color.BLUE) - Color.blue(
                            bitmap.getPixel(
                                coords[0] + width / 2,
                                coords[1] + height / 2
                            )
                        )
                    ) < 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testUsageFlagContainsFrontBufferUsage() {
        val usageFlags = GLFrontBufferedRenderer.obtainHardwareBufferUsageFlags()
        if (UsageFlagsVerificationHelper.isSupported(HardwareBuffer.USAGE_FRONT_BUFFER)) {
            assertNotEquals(0, usageFlags and HardwareBuffer.USAGE_FRONT_BUFFER)
        } else {
            assertEquals(0, usageFlags and HardwareBuffer.USAGE_FRONT_BUFFER)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.TIRAMISU)
    fun testUsageFlagContainsComposerOverlay() {
        val usageFlags = GLFrontBufferedRenderer.obtainHardwareBufferUsageFlags()
        if (UsageFlagsVerificationHelper.isSupported(HardwareBuffer.USAGE_COMPOSER_OVERLAY)) {
            assertNotEquals(
                0,
                usageFlags and HardwareBuffer.USAGE_COMPOSER_OVERLAY
            )
        } else {
            assertEquals(0, usageFlags and HardwareBuffer.USAGE_COMPOSER_OVERLAY)
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testBaseFlags() {
        assertNotEquals(
            0, GLFrontBufferedRenderer.BaseFlags and
                HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE
        )
        assertNotEquals(
            0, GLFrontBufferedRenderer.BaseFlags and
                HardwareBuffer.USAGE_GPU_COLOR_OUTPUT
        )
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun testRenderFrontBufferSeveralTimes() {

        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {

            var red = 1f
            var blue = 0f
            val mOrthoMatrix = FloatArray(16)
            val mProjectionMatrix = FloatArray(16)
            var mRectangle: Rectangle? = null

            private fun getSquare(): Rectangle = mRectangle ?: Rectangle().also { mRectangle = it }

            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                val color = Color.argb(1f, red, 0f, blue)
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                getSquare().draw(mProjectionMatrix, color, 0f, 0f, 100f, 100f)

                val tmp = red
                red = blue
                blue = tmp
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                val color = Color.argb(1f, red, 0f, blue)
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                getSquare().draw(mProjectionMatrix, color, 0f, 0f, 100f, 100f)
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    renderer = GLFrontBufferedRenderer(it.getSurfaceView(), callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                val param = Any()
                repeat(500) {
                    renderer?.renderFrontBufferedLayer(param)
                }
            }
        } finally {
            renderer.blockingRelease(10000)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testDoubleBufferedContentsNotPersisted() {
        val mOrthoMatrix = FloatArray(16)
        val mProjectionMatrix = FloatArray(16)
        val screenWidth = FrontBufferedRendererTestActivity.WIDTH
        val rectWidth = 10f

        val renderLatch = CountDownLatch(1)
        val firstDrawLatch = CountDownLatch(1)
        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {
            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                val left = screenWidth / 4 + (param as Float) - rectWidth / 2
                val top = 0f
                val right = left + rectWidth / 2
                val bottom = 100f

                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.RED, left, top, right, bottom)
                firstDrawLatch.countDown()
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                for (param in params) {
                    val left = screenWidth / 4 + (param as Float) - rectWidth / 2
                    val top = 0f
                    val right = left + rectWidth / 2
                    val bottom = 100f

                    Rectangle().draw(mProjectionMatrix, Color.RED, left, top, right, bottom)
                    assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
                }
            }

            override fun onDoubleBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        })
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(0f)
                renderer?.commit()
                renderer?.renderFrontBufferedLayer(screenWidth / 2f)
                renderer?.commit()
            }

            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                (bitmap.getPixel(
                    coords[0] + width / 4, coords[1] + height / 2
                ) == Color.BLACK) &&
                    (bitmap.getPixel(
                        coords[0] + 3 * width / 4 - 1,
                        coords[1] + height / 2
                    ) == Color.RED)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    @Test
    fun testRenderAfterPauseAndResume() {
        val renderLatch = CountDownLatch(2)
        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {

            val mProjectionMatrix = FloatArray(16)
            val mOrthoMatrix = FloatArray(16)

            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                Rectangle().draw(mProjectionMatrix, Color.BLUE, 0f, 0f, 100f, 100f)
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    transaction.addTransactionCommittedListener(
                        Executors.newSingleThreadExecutor(),
                        object : SurfaceControlCompat.TransactionCommittedListener {
                            override fun onTransactionCommitted() {
                                renderLatch.countDown()
                            }
                        }
                    )
                } else {
                    renderLatch.countDown()
                }
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        var surfaceView: SurfaceView? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    renderer = GLFrontBufferedRenderer(surfaceView!!, callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }
            // Navigate to stopped and resumed state to simulate returning to the application
            scenario.moveToState(Lifecycle.State.CREATED)
                .moveToState(Lifecycle.State.RESUMED)
                .onActivity {
                    renderer?.renderFrontBufferedLayer(Any())
                }
            assertTrue(renderLatch.await(3000, TimeUnit.MILLISECONDS))

            val coords = IntArray(2)
            val width: Int
            val height: Int
            with(surfaceView!!) {
                getLocationOnScreen(coords)
                width = this.width
                height = this.height
            }

            SurfaceControlUtils.validateOutput { bitmap ->
                Color.RED ==
                    bitmap.getPixel(coords[0] + width / 2, coords[1] + height / 2)
            }
        } finally {
            renderer.blockingRelease()
        }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
    fun test180DegreeRotationBufferTransform() {
        val initialFrontBufferLatch = CountDownLatch(1)
        val secondFrontBufferLatch = CountDownLatch(1)
        var bufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
        var surfaceView: SurfaceView? = null
        val surfaceHolderCallbacks = object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                // NO-OP
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
                bufferTransform =
                    BufferTransformHintResolver().getBufferTransformHint(surfaceView!!)
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                // NO-OP
            }
        }
        var configuredBufferTransform = BufferTransformHintResolver.UNKNOWN_TRANSFORM
        val callbacks = object : GLFrontBufferedRenderer.Callback<Any> {

            val mOrthoMatrix = FloatArray(16)
            val mProjectionMatrix = FloatArray(16)
            var mRectangle: Rectangle? = null

            private fun getSquare(): Rectangle = mRectangle ?: Rectangle().also { mRectangle = it }

            override fun onDrawFrontBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                param: Any
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                getSquare().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onDrawDoubleBufferedLayer(
                eglManager: EGLManager,
                bufferWidth: Int,
                bufferHeight: Int,
                transform: FloatArray,
                params: Collection<Any>
            ) {
                GLES20.glViewport(0, 0, bufferWidth, bufferHeight)
                Matrix.orthoM(
                    mOrthoMatrix,
                    0,
                    0f,
                    bufferWidth.toFloat(),
                    0f,
                    bufferHeight.toFloat(),
                    -1f,
                    1f
                )
                Matrix.multiplyMM(mProjectionMatrix, 0, mOrthoMatrix, 0, transform, 0)
                getSquare().draw(mProjectionMatrix, Color.RED, 0f, 0f, 100f, 100f)
            }

            override fun onFrontBufferedLayerRenderComplete(
                frontBufferedLayerSurfaceControl: SurfaceControlCompat,
                transaction: SurfaceControlCompat.Transaction
            ) {
                configuredBufferTransform =
                    transaction.mBufferTransforms[frontBufferedLayerSurfaceControl]
                        ?: BufferTransformHintResolver.UNKNOWN_TRANSFORM
                if (initialFrontBufferLatch.count == 0L) {
                    secondFrontBufferLatch.countDown()
                }
                initialFrontBufferLatch.countDown()
            }
        }
        var renderer: GLFrontBufferedRenderer<Any>? = null
        try {
            val scenario = ActivityScenario.launch(FrontBufferedRendererTestActivity::class.java)
                .moveToState(Lifecycle.State.CREATED)
                .onActivity {
                    surfaceView = it.getSurfaceView()
                    it.getSurfaceView().holder.addCallback(surfaceHolderCallbacks)
                    renderer = GLFrontBufferedRenderer(it.getSurfaceView(), callbacks)
                }

            scenario.moveToState(Lifecycle.State.RESUMED).onActivity {
                renderer?.renderFrontBufferedLayer(Any())
            }

            assertTrue(initialFrontBufferLatch.await(3000, TimeUnit.MILLISECONDS))

            val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
            assertTrue(automation.setRotation(UiAutomation.ROTATION_FREEZE_180))
            automation.waitForIdle(1000, 3000)

            renderer?.renderFrontBufferedLayer(Any())

            assertTrue(secondFrontBufferLatch.await(3000, TimeUnit.MILLISECONDS))

            assertEquals(
                BufferTransformer().invertBufferTransform(bufferTransform),
                configuredBufferTransform
            )
        } finally {
            renderer.blockingRelease(10000)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun GLFrontBufferedRenderer<*>?.blockingRelease(timeoutMillis: Long = 3000) {
        if (this != null) {
            val destroyLatch = CountDownLatch(1)
            release(false) {
                destroyLatch.countDown()
            }
            assertTrue(destroyLatch.await(timeoutMillis, TimeUnit.MILLISECONDS))
        } else {
            fail("GLFrontBufferedRenderer is not initialized")
        }
    }
}