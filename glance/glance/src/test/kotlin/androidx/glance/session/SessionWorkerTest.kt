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

package androidx.glance.session

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.glance.EmittableWithChildren
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.layout.Box
import androidx.glance.layout.EmittableBox
import androidx.glance.text.EmittableText
import androidx.glance.text.Text
import androidx.test.core.app.ApplicationProvider
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionWorkerTest {
    private val sessionManager = TestSessionManager()
    private lateinit var context: Context
    private lateinit var worker: SessionWorker

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        worker = TestListenableWorkerBuilder<SessionWorker>(context)
            .setInputData(Data(mapOf(sessionManager.keyParam to SESSION_KEY)))
            .build()
            .also {
                it.sessionManager = sessionManager
            }
    }

    @Test
    fun createSessionWorker() = runBlocking {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }
        sessionManager.startSession(context)
        sessionManager.closeSession()
    }

    @Test
    fun sessionWorkerRunsComposition() = runBlocking {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val root = sessionManager.startSession(context) {
            Box {
                Text("Hello World")
            }
        }.first()
        val box = assertIs<EmittableBox>(root.children.single())
        val text = assertIs<EmittableText>(box.children.single())
        assertThat(text.text).isEqualTo("Hello World")
        sessionManager.closeSession()
    }

    @Test
    fun sessionWorkerCallsProvideGlance(): Unit = runBlocking {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }
        sessionManager.startSession(context).first()
        val session = assertIs<TestSession>(sessionManager.getSession(SESSION_KEY))
        assertThat(session.provideGlanceCalled).isEqualTo(1)
        sessionManager.closeSession()
    }

    @Test
    fun sessionWorkerStateChangeTriggersRecomposition() = runBlocking {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val state = mutableStateOf("Hello World")
        val uiFlow = sessionManager.startSession(context) {
                Text(state.value)
        }
        uiFlow.first().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }

        state.value = "Hello Earth"
        uiFlow.first().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello Earth")
        }
        sessionManager.closeSession()
    }

    @Test
    fun sessionWorkerReceivesActions() = runBlocking {
        launch {
            val result = worker.doWork()
            assertThat(result).isEqualTo(Result.success())
        }

        val state = mutableStateOf("Hello World")
        val uiFlow = sessionManager.startSession(context) {
            Text(state.value)
        }
        uiFlow.first().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello World")
        }
        val session = assertNotNull(sessionManager.getSession(SESSION_KEY))
        session.sendEvent {
            state.value = "Hello Earth"
        }
        uiFlow.first().let { root ->
            val text = assertIs<EmittableText>(root.children.single())
            assertThat(text.text).isEqualTo("Hello Earth")
        }
        sessionManager.closeSession()
    }
}

private const val SESSION_KEY = "123"

class TestSessionManager : SessionManager {
    private val sessions = mutableMapOf<String, Session>()

    suspend fun startSession(
        context: Context,
        content: @GlanceComposable @Composable () -> Unit = {}
    ) = MutableSharedFlow<EmittableWithChildren>().also { flow ->
        startSession(context, TestSession(onUiFlow = flow, content = content))
    }

    suspend fun closeSession() {
        closeSession(SESSION_KEY)
    }

    override suspend fun startSession(context: Context, session: Session) {
        sessions[session.key] = session
    }

    override suspend fun closeSession(key: String) {
        sessions[key]?.close()
    }

    override suspend fun isSessionRunning(context: Context, key: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getSession(key: String): Session? = sessions[key]
}

class TestSession(
    key: String = SESSION_KEY,
    val onUiFlow: MutableSharedFlow<EmittableWithChildren>? = null,
    val content: @GlanceComposable @Composable () -> Unit = {},
) : Session(key) {
    override fun createRootEmittable() = object : EmittableWithChildren() {
        override var modifier: GlanceModifier = GlanceModifier
        override fun copy() = this
        override fun toString() = "EmittableRoot(children=[\n${childrenToString()}\n])"
    }

    var provideGlanceCalled = 0
    override suspend fun provideGlance(
        context: Context,
        setContent: SetContentFn
    ) {
        provideGlanceCalled++
        setContent(content)
    }

    override suspend fun processEmittableTree(context: Context, root: EmittableWithChildren) {
        onUiFlow?.emit(root)
    }

    override suspend fun processEvent(context: Context, event: Any) {
        require(event is Function0<*>)
        event.invoke()
    }
}