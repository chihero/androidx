/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.lifecycle;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.HashMap;
import java.util.Map;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class MethodCallsLogger {
    private Map<String, Integer> mCalledMethods = new HashMap<>();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    public boolean approveCall(@NonNull String name, int type) {
        Integer nullableMask = mCalledMethods.get(name);
        int mask = nullableMask != null ? nullableMask : 0;
        boolean wasCalled = (mask & type) != 0;
        mCalledMethods.put(name, mask | type);
        return !wasCalled;
    }
}
