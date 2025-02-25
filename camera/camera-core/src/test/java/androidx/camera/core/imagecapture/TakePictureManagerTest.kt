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

package androidx.camera.core.imagecapture

import android.os.Build
import android.os.Looper.getMainLooper
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.ERROR_CAMERA_CLOSED
import androidx.camera.core.ImageCapture.ERROR_CAPTURE_FAILED
import androidx.camera.core.ImageCapture.OutputFileResults
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.impl.CaptureConfig
import androidx.camera.testing.fakes.FakeImageInfo
import androidx.camera.testing.fakes.FakeImageProxy
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

/**
 * Unit tests for [TakePictureManager].
 */
@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = Build.VERSION_CODES.LOLLIPOP)
class TakePictureManagerTest {

    private val imagePipeline = FakeImagePipeline()
    private val imageCaptureControl = FakeImageCaptureControl()
    private val takePictureManager =
        TakePictureManager(imageCaptureControl).also { it.imagePipeline = imagePipeline }
    private val exception = ImageCaptureException(ImageCapture.ERROR_UNKNOWN, "", null)

    @After
    fun tearDown() {
        imagePipeline.close()
        imageCaptureControl.clear()
    }

    @Test
    fun pause_inFlightRequestRetried() {
        // Arrange: create 2 requests and offer them to the manager.
        imageCaptureControl.shouldUsePendingResult = true
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)
        assertThat(takePictureManager.mNewRequests).containsExactly(request2)

        // Act: pause the manager.
        takePictureManager.pause()
        shadowOf(getMainLooper()).idle()

        // Assert: the in-flight request is retried.
        assertThat(takePictureManager.mNewRequests).containsExactly(request1, request2).inOrder()
        assertThat(request1.remainingRetries).isEqualTo(0)
    }

    @Test
    fun abort_captureRequestFutureIsCanceled() {
        // Arrange: configure ImageCaptureControl to not return immediately.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        imageCaptureControl.shouldUsePendingResult = true

        // Act: offer request then abort.
        takePictureManager.offerRequest(request)
        takePictureManager.abortRequests()
        shadowOf(getMainLooper()).idle()

        // Assert: that the app receives exception and the capture future is canceled.
        assertThat((request.exceptionReceived as ImageCaptureException).imageCaptureError)
            .isEqualTo(ERROR_CAMERA_CLOSED)
        assertThat(imageCaptureControl.pendingResult.isCancelled).isTrue()
    }

    @Test
    fun abortPostProcessingRequests_receiveErrorCallback() {
        // Arrange: setup a request that is captured but not processed.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)
        val processingRequest = imagePipeline.getProcessingRequest(request)
        processingRequest.onImageCaptured()
        shadowOf(getMainLooper()).idle()
        // Verify the "captured, not processed" status.
        assertThat(takePictureManager.hasCapturingRequest()).isFalse()
        assertThat(takePictureManager.incompleteRequests[0].captureFuture.isDone).isTrue()
        assertThat(takePictureManager.incompleteRequests[0].completeFuture.isDone).isFalse()

        // Act: abort all requests.
        takePictureManager.abortRequests()
        shadowOf(getMainLooper()).idle()

        // Assert: the user has received a CAMERA_CLOSED error.
        assertThat((request.exceptionReceived as ImageCaptureException).imageCaptureError)
            .isEqualTo(ERROR_CAMERA_CLOSED)
    }

    @Test
    fun abortNewRequests_receiveErrorCallback() {
        // Arrange: send 2 request.
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)

        // Act: abort the manager and finish the 1st request.
        takePictureManager.abortRequests()
        // Camera returns the image, but it should be ignored.
        val processingRequest = imagePipeline.getProcessingRequest(request1)
        processingRequest.onImageCaptured()
        processingRequest.onFinalResult(FakeImageProxy(FakeImageInfo()))
        shadowOf(getMainLooper()).idle()

        // Assert: one request is sent.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
        // Both request are aborted.
        assertThat((request1.exceptionReceived as ImageCaptureException).imageCaptureError)
            .isEqualTo(ERROR_CAMERA_CLOSED)
        assertThat((request2.exceptionReceived as ImageCaptureException).imageCaptureError)
            .isEqualTo(ERROR_CAMERA_CLOSED)
        assertThat(takePictureManager.mNewRequests).isEmpty()
    }

    @Test(expected = IllegalStateException::class)
    fun callOnFailureTwice_throwsException() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)
        val processingRequest = imagePipeline.getProcessingRequest(request)
        processingRequest.onImageCaptured()
        // Act.
        processingRequest.onProcessFailure(exception)
        processingRequest.onProcessFailure(exception)
    }

    @Test(expected = IllegalStateException::class)
    fun callOnFinalResultTwice_throwsException() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)
        val processingRequest = imagePipeline.getProcessingRequest(request)
        processingRequest.onImageCaptured()
        // Act.
        processingRequest.onFinalResult(FakeImageProxy(FakeImageInfo()))
        processingRequest.onFinalResult(OutputFileResults(null))
    }

    @Test
    fun pause_requestsPausedUntilResumed() {
        // Arrange: send 2 request.
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)

        // Act: pause the manage and finish the 1st request.
        takePictureManager.pause()
        imagePipeline.getProcessingRequest(request1).onImageCaptured()
        shadowOf(getMainLooper()).idle()

        // Assert: only one request is sent.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()

        // Act: resume to process the 2nd request.
        takePictureManager.resume()
        shadowOf(getMainLooper()).idle()

        // Assert: 2nd request is sent too.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
    }

    @Test
    fun submitRequestFailsWithImageCaptureException_appGetsTheSameException() {
        // Arrange: configure ImageCaptureControl to always fail.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        imageCaptureControl.shouldUsePendingResult = true
        imageCaptureControl.pendingResultCompleter.setException(exception)

        // Act.
        takePictureManager.offerRequest(request)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.exceptionReceived!!).isEqualTo(exception)
        assertThat(takePictureManager.hasCapturingRequest()).isFalse()
    }

    @Test
    fun submitRequestFailsWithGenericException_appGetsWrappedException() {
        // Arrange: configure ImageCaptureControl to always fail.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val genericException = Exception()
        imageCaptureControl.shouldUsePendingResult = true
        imageCaptureControl.pendingResultCompleter.setException(genericException)

        // Act.
        takePictureManager.offerRequest(request)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.exceptionReceived!!.imageCaptureError).isEqualTo(ERROR_CAPTURE_FAILED)
        assertThat(request.exceptionReceived!!.cause).isEqualTo(genericException)
        assertThat(takePictureManager.hasCapturingRequest()).isFalse()
    }

    @Test
    fun submitTwoRequests_ImageCaptureControlCalledInOrder() {
        // Arrange: setup ImagePipeline request and response
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val response1 = listOf(CaptureConfig.defaultEmptyCaptureConfig())
        val response2 = listOf(
            CaptureConfig.defaultEmptyCaptureConfig(),
            CaptureConfig.defaultEmptyCaptureConfig()
        )
        imagePipeline.captureConfigMap[request1] = response1
        imagePipeline.captureConfigMap[request2] = response2

        // Act: offer 2 requests
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)
        shadowOf(getMainLooper()).idle()

        // Assert:
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
        assertThat(imageCaptureControl.latestCaptureConfigs).isEqualTo(response1)
        assertThat(takePictureManager.mNewRequests.single()).isEqualTo(request2)

        // Act: invoke image captured so TakePictureManager can issue another request.
        imagePipeline.getProcessingRequest(request1).onImageCaptured()
        shadowOf(getMainLooper()).idle()

        // Assert: imageCaptureControl was invoked in the exact given order.
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
        assertThat(imageCaptureControl.latestCaptureConfigs).isEqualTo(response2)
        assertThat(takePictureManager.mNewRequests).isEmpty()
    }

    /**
     * When post-processing results come back in a different order as they are being sent.
     */
    @Test
    fun pipelineReturnsMultipleResponsesOutOfOrder_appReceivesCorrectly() {
        // Arrange: setup 3 requests and their responses in the order of 1->2->3.
        val request1 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        val request2 = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        val request3 = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request1)
        takePictureManager.offerRequest(request2)
        takePictureManager.offerRequest(request3)
        shadowOf(getMainLooper()).idle()
        val response1 = exception
        val response2 = OutputFileResults(null)
        val response3 = FakeImageProxy(FakeImageInfo())
        imagePipeline.getProcessingRequest(request1).onImageCaptured()
        imagePipeline.getProcessingRequest(request2).onImageCaptured()
        imagePipeline.getProcessingRequest(request3).onImageCaptured()

        // Act: send the responses in the order of 3->1->2
        imagePipeline.getProcessingRequest(request3).onFinalResult(response3)
        imagePipeline.getProcessingRequest(request1).onProcessFailure(response1)
        imagePipeline.getProcessingRequest(request2).onFinalResult(response2)
        shadowOf(getMainLooper()).idle()

        // Assert: responses received correctly
        assertThat(request1.exceptionReceived).isEqualTo(response1)
        assertThat(request2.fileReceived).isEqualTo(response2)
        assertThat(request3.imageReceived).isEqualTo(response3)
    }

    @Test
    fun pipelineReturnsError_appReceivesError() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)

        // Act: send exception via ImagePipeline
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onProcessFailure(exception)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.exceptionReceived).isEqualTo(exception)
    }

    @Test
    fun pipelineReturnsOnDiskResult_receivedByApp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.ON_DISK)
        takePictureManager.offerRequest(request)

        // Act: send OutputFileResults via ImagePipeline
        val outputFileResults = OutputFileResults(null)
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onFinalResult(outputFileResults)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.fileReceived).isEqualTo(outputFileResults)
    }

    @Test
    fun pipelineReturnsInMemoryResult_receivedByApp() {
        // Arrange.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)

        // Act: send ImageProxy via ImagePipeline
        val image = FakeImageProxy(FakeImageInfo())
        imagePipeline.getProcessingRequest(request).onImageCaptured()
        imagePipeline.getProcessingRequest(request).onFinalResult(image)
        shadowOf(getMainLooper()).idle()

        // Assert.
        assertThat(request.imageReceived).isEqualTo(image)
    }

    @Test
    fun takePictureManager_unableToProcessNextWhenOverMaxImages() {
        // Arrange.
        imagePipeline.queueCapacity = 0

        // Act: send the request.
        val request = FakeTakePictureRequest(FakeTakePictureRequest.Type.IN_MEMORY)
        takePictureManager.offerRequest(request)

        // Assert: the request is blocked.
        assertThat(takePictureManager.mNewRequests.size).isEqualTo(1)
        assertThat(imageCaptureControl.actions).isEmpty()

        // Act: increase the capacity and invoke image closed.
        imagePipeline.queueCapacity = 1
        takePictureManager.onImageClose(FakeImageProxy(FakeImageInfo()))
        shadowOf(getMainLooper()).idle()

        // Assert: the request is sent.
        assertThat(takePictureManager.mNewRequests.size).isEqualTo(0)
        assertThat(imageCaptureControl.actions).containsExactly(
            FakeImageCaptureControl.Action.LOCK_FLASH,
            FakeImageCaptureControl.Action.SUBMIT_REQUESTS,
            FakeImageCaptureControl.Action.UNLOCK_FLASH,
        ).inOrder()
    }
}