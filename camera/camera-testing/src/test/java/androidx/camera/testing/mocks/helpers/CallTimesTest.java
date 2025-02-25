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

package androidx.camera.testing.mocks.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CallTimesTest {
    private final CallTimes mCallTimes = new CallTimes(5);

    @Test
    public void getTimesReturnsCorrectly() {
        assertEquals(5, mCallTimes.getTimes());
    }

    @Test
    public void actualCallCountMatchesExactly_isSatisfiedReturnsTrue() {
        assertTrue(mCallTimes.isSatisfied(5));
    }

    @Test
    public void actualCallCountIsLess_isSatisfiedReturnsFalse() {
        assertFalse(mCallTimes.isSatisfied(2));
    }

    @Test
    public void actualCallCountIsMore_isSatisfiedReturnsFalse() {
        assertFalse(mCallTimes.isSatisfied(8));
    }
}
