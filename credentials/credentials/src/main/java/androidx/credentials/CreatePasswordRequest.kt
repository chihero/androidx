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

package androidx.credentials

import android.os.Bundle
import androidx.annotation.VisibleForTesting

/**
 * A request to save the user password credential with their password provider.
 *
 * @property id the user id associated with the password
 * @property password the password
 * @throws NullPointerException If [id] is null
 * @throws NullPointerException If [password] is null
 * @throws IllegalArgumentException If [password] is empty
 */
class CreatePasswordRequest constructor(
    val id: String,
    val password: String,
) : CreateCredentialRequest(
    PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
    toBundle(id, password),
    false,
) {

    init {
        require(password.isNotEmpty()) { "password should not be empty" }
    }

    /** @hide */
    companion object {
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val BUNDLE_KEY_ID = "androidx.credentials.BUNDLE_KEY_ID"
        @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
        const val BUNDLE_KEY_PASSWORD = "androidx.credentials.BUNDLE_KEY_PASSWORD"

        @JvmStatic
        internal fun toBundle(id: String, password: String): Bundle {
            val bundle = Bundle()
            bundle.putString(BUNDLE_KEY_ID, id)
            bundle.putString(BUNDLE_KEY_PASSWORD, password)
            return bundle
        }
    }
}