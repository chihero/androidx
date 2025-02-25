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

package androidx.glance.appwidget

import android.Manifest
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.view.children
import androidx.glance.session.GlanceSessionManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertIs
import kotlin.test.fail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

@SdkSuppress(minSdkVersion = 29)
class AppWidgetHostRule(
    private var mPortraitSize: DpSize = DpSize(200.dp, 300.dp),
    private var mLandscapeSize: DpSize = DpSize(300.dp, 200.dp),
    private var useSessionManager: Boolean = false,
) : TestRule {

    val portraitSize: DpSize
        get() = mPortraitSize
    val landscapeSize: DpSize
        get() = mLandscapeSize

    private val mInstrumentation = InstrumentationRegistry.getInstrumentation()
    private val mUiAutomation = mInstrumentation.uiAutomation

    private val mActivityRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    private val mUiDevice = UiDevice.getInstance(mInstrumentation)

    // Ensure the screen starts in portrait and restore the orientation on leaving
    private val mOrientationRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                mUiDevice.freezeRotation()
                mUiDevice.setOrientationNatural()
                base.evaluate()
                mUiDevice.unfreezeRotation()
            }
        }
    }

    private val mInnerRules = RuleChain.outerRule(mActivityRule).around(mOrientationRule)

    private var mHostStarted = false
    private var mMaybeHostView: TestAppWidgetHostView? = null
    private var mAppWidgetId = 0
    private val mScenario: ActivityScenario<AppWidgetHostTestActivity>
        get() = mActivityRule.scenario
    private val mContext = ApplicationProvider.getApplicationContext<Context>()

    val mHostView: TestAppWidgetHostView
        get() = checkNotNull(mMaybeHostView) { "No app widget installed on the host" }

    val appWidgetId: Int get() = mAppWidgetId

    val device: UiDevice get() = mUiDevice

    override fun apply(base: Statement, description: Description) = object : Statement() {

        override fun evaluate() {
            WorkManagerTestInitHelper.initializeTestWorkManager(mContext)
            TestGlanceAppWidget.sessionManager =
                if (useSessionManager) GlanceSessionManager else null
            mInnerRules.apply(base, description).evaluate()
            stopHost()
        }

        private fun stopHost() {
            if (mHostStarted) {
                mUiAutomation.dropShellPermissionIdentity()
            }
            WorkManager.getInstance(mContext).cancelAllWork()
        }
    }

    /** Start the host and bind the app widget. */
    fun startHost() {
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)
        mHostStarted = true

        mActivityRule.scenario.onActivity { activity ->
            mMaybeHostView = activity.bindAppWidget(mPortraitSize, mLandscapeSize)
        }

        val hostView = checkNotNull(mMaybeHostView) { "Host view wasn't successfully started" }

        runAndWaitForChildren {
            mAppWidgetId = hostView.appWidgetId
            hostView.waitForRemoteViews()
        }
    }

    /**
     * Run the [block] (usually some sort of app widget update) and wait for new RemoteViews to be
     * applied.
     *
     * This should not be called from the main thread, i.e. in [onHostView] or [onHostActivity].
     */
    suspend fun runAndWaitForUpdate(block: suspend () -> Unit) {
        val hostView = checkNotNull(mMaybeHostView) { "Host view wasn't successfully started" }
        hostView.resetRemoteViewsLatch()
        withContext(Dispatchers.Main) { block() }
        // Do not wait on the main thread so that the UI handlers can run.
        runAndWaitForChildren {
            hostView.waitForRemoteViews()
        }
    }

    /**
     * Set TestGlanceAppWidgetReceiver to ignore broadcasts, run [block], and then reset
     * TestGlanceAppWidgetReceiver.
     */
    fun ignoreBroadcasts(block: () -> Unit) {
        TestGlanceAppWidgetReceiver.ignoreBroadcasts = true
        try {
            block()
        } finally {
            TestGlanceAppWidgetReceiver.ignoreBroadcasts = false
        }
    }

    fun removeAppWidget() {
        mActivityRule.scenario.onActivity { activity ->
            val hostView = checkNotNull(mMaybeHostView) { "No app widget to remove" }
            activity.deleteAppWidget(hostView)
        }
    }

    fun onHostActivity(block: (AppWidgetHostTestActivity) -> Unit) {
        mScenario.onActivity(block)
    }

    fun onHostView(block: (AppWidgetHostView) -> Unit) {
        onHostActivity { block(mHostView) }
    }

    /**
     * The top-level view is always boxed into a FrameLayout.
     *
     * This will retrieve the actual top-level view, skipping the boxing for the root view, and
     * possibly the one to get the exact size.
     */
    inline fun <reified T : View> onUnboxedHostView(crossinline block: (T) -> Unit) {
        onHostActivity {
            val boxingView = assertIs<ViewGroup>(mHostView.getChildAt(0))
            block(boxingView.children.single().getTargetView())
        }
    }

    /** Change the orientation to landscape.*/
    fun setLandscapeOrientation() {
        var activity: AppWidgetHostTestActivity? = null
        onHostActivity {
            it.resetConfigurationChangedLatch()
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            activity = it
        }
        checkNotNull(activity).apply {
            waitForConfigurationChange()
            assertThat(lastConfiguration.orientation).isEqualTo(Configuration.ORIENTATION_LANDSCAPE)
        }
    }

    /** Change the orientation to portrait.*/
    fun setPortraitOrientation() {
        var activity: AppWidgetHostTestActivity? = null
        onHostActivity {
            it.resetConfigurationChangedLatch()
            it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity = it
        }
        checkNotNull(activity).apply {
            waitForConfigurationChange()
            assertThat(lastConfiguration.orientation).isEqualTo(Configuration.ORIENTATION_PORTRAIT)
        }
    }

    /**
     * Set the sizes for portrait and landscape for the host view.
     *
     * If specified, the options bundle for the AppWidget is updated and the code waits for the
     * new RemoteViews from the provider.
     *
     * @param portraitSize Size of the view in portrait mode.
     * @param landscapeSize Size of the view in landscape. If null, the portrait and landscape sizes
     *   will be set to be such that portrait is narrower than tall and the landscape wider than
     *   tall.
     * @param updateRemoteViews If the host is already started and this is true, the provider will
     *   be called to get a new set of RemoteViews for the new sizes.
     */
    fun setSizes(
        portraitSize: DpSize,
        landscapeSize: DpSize? = null,
        updateRemoteViews: Boolean = true
    ) {
        val (portrait, landscape) = if (landscapeSize != null) {
            portraitSize to landscapeSize
        } else {
            if (portraitSize.width < portraitSize.height) {
                portraitSize to DpSize(portraitSize.height, portraitSize.width)
            } else {
                DpSize(portraitSize.height, portraitSize.width) to portraitSize
            }
        }
        mLandscapeSize = landscape
        mPortraitSize = portrait
        if (!mHostStarted) return

        val hostView = mMaybeHostView
        if (hostView != null) {
            mScenario.onActivity {
                hostView.setSizes(portrait, landscape)
            }

            if (updateRemoteViews) {
                runAndWaitForChildren {
                    hostView.resetRemoteViewsLatch()
                    AppWidgetManager.getInstance(mContext).updateAppWidgetOptions(
                        mAppWidgetId,
                        optionsBundleOf(listOf(portrait, landscape))
                    )
                    hostView.waitForRemoteViews()
                }
            }
        }
    }

    fun runAndObserveUntilDraw(
        condition: String = "Expected condition to be met within 5 seconds",
        run: () -> Unit = {},
        test: () -> Boolean
    ) {
        val hostView = mHostView
        val latch = CountDownLatch(1)
        val onDrawListener = ViewTreeObserver.OnDrawListener {
            if (hostView.childCount > 0 && test()) latch.countDown()
        }
        mActivityRule.scenario.onActivity {
            hostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        run()

        try {
            if (test()) return
            val interval = 200L
            for (timeout in 0..5000L step interval) {
                val countedDown = latch.await(interval, TimeUnit.MILLISECONDS)
                if (countedDown || test()) return
            }
            fail(condition)
        } finally {
            latch.countDown() // make sure it's released in all conditions
            mActivityRule.scenario.onActivity {
                hostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
            }
        }
    }

    private fun runAndWaitForChildren(action: () -> Unit) {
        runAndObserveUntilDraw("Expected new children on HostView within 5 seconds", action) {
            mHostView.childCount > 0
        }
    }
}
