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

package androidx.benchmark.macro.perfetto

import androidx.annotation.RestrictTo
import androidx.benchmark.macro.perfetto.server.QueryResultIterator

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class Slice(
    val name: String,
    val ts: Long,
    val dur: Long
) {
    val endTs: Long = ts + dur
    val frameId = name.substringAfterLast(" ").toIntOrNull()

    fun contains(targetTs: Long): Boolean {
        return targetTs >= ts && targetTs <= (ts + dur)
    }
}

/**
 * Convenient function to immediately retrieve a list of slices.
 * Note that this method is provided for convenience and exhausts the iterator.
 */
internal fun QueryResultIterator.toSlices(): List<Slice> =
    toList {
        Slice(name = it["name"] as String, ts = it["ts"] as Long, dur = it["dur"] as Long)
    }