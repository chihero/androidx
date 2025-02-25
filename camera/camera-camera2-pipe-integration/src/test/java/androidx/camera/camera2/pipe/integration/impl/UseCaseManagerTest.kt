/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.camera.camera2.pipe.integration.impl

import android.os.Build
import androidx.camera.camera2.pipe.CameraId
import androidx.camera.camera2.pipe.integration.adapter.RobolectricCameraPipeTestRunner
import androidx.camera.camera2.pipe.integration.config.CameraConfig
import androidx.camera.camera2.pipe.integration.interop.Camera2CameraControl
import androidx.camera.camera2.pipe.integration.interop.ExperimentalCamera2Interop
import androidx.camera.camera2.pipe.integration.testing.FakeCamera2CameraControlCompat
import androidx.camera.camera2.pipe.integration.testing.FakeCameraProperties
import androidx.camera.camera2.pipe.integration.testing.FakeUseCaseCameraComponentBuilder
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(RobolectricCameraPipeTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class UseCaseManagerTest {
    private val useCaseThreads by lazy {
        val dispatcher = Dispatchers.Default
        val cameraScope = CoroutineScope(
            Job() +
                dispatcher
        )

        UseCaseThreads(
            cameraScope,
            dispatcher.asExecutor(),
            dispatcher
        )
    }

    @Test
    fun enabledUseCasesEmpty_whenUseCaseAttachedOnly() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = Preview.Builder().build()

        // Act
        useCaseManager.attach(listOf(useCase))

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @Test
    fun enabledUseCasesNotEmpty_whenUseCaseEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val useCase = Preview.Builder().build()
        useCaseManager.attach(listOf(useCase))

        // Act
        useCaseManager.activate(useCase)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).containsExactly(useCase)
    }

    @Test
    fun meteringRepeatingNotEnabled_whenPreviewEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(preview, imageCapture))

        // Act
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_whenOnlyImageCaptureEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))

        // Act
        useCaseManager.activate(imageCapture)

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.runningUseCases?.map { it::class.java }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun meteringRepeatingDisabled_whenPreviewBecomesEnabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        val preview = Preview.Builder().build()
        useCaseManager.attach(listOf(preview))
        useCaseManager.activate(preview)

        // Assert
        val activeUseCases = useCaseManager.camera?.runningUseCases
        assertThat(activeUseCases).containsExactly(preview, imageCapture)
    }

    @Test
    fun meteringRepeatingEnabled_afterAllUseCasesButImageCaptureDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val preview = Preview.Builder().build()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(preview, imageCapture))
        useCaseManager.activate(preview)
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.deactivate(preview)

        // Assert
        val enabledUseCaseClasses = useCaseManager.camera?.runningUseCases?.map { it::class.java }
        assertThat(enabledUseCaseClasses).containsExactly(
            ImageCapture::class.java,
            MeteringRepeating::class.java
        )
    }

    @Test
    fun meteringRepeatingDisabled_whenAllUseCasesDisabled() {
        // Arrange
        val useCaseManager = createUseCaseManager()
        val imageCapture = ImageCapture.Builder().build()
        useCaseManager.attach(listOf(imageCapture))
        useCaseManager.activate(imageCapture)

        // Act
        useCaseManager.deactivate(imageCapture)

        // Assert
        val enabledUseCases = useCaseManager.camera?.runningUseCases
        assertThat(enabledUseCases).isEmpty()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    @Suppress("UNCHECKED_CAST", "PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun createUseCaseManager() = UseCaseManager(
        cameraConfig = CameraConfig(CameraId("0")),
        builder = FakeUseCaseCameraComponentBuilder(),
        controls = HashSet<UseCaseCameraControl>() as java.util.Set<UseCaseCameraControl>,
        cameraProperties = FakeCameraProperties(),
        camera2CameraControl = Camera2CameraControl.create(
            FakeCamera2CameraControlCompat(),
            useCaseThreads,
            ComboRequestListener()
        ),
        displayInfoManager = DisplayInfoManager(ApplicationProvider.getApplicationContext())
    )
}
