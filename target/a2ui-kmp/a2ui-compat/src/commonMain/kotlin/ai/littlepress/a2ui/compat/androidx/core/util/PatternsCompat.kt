/*
 * Copyright (C) 2016 The Android Open Source Project
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
// a2ui-kmp: the part of androidx.core.util.PatternsCompat (androidx.core 1.19.0) that upstream
// A2UI uses, for desktop and iOS, where androidx.core is not available. Same name, so
// the overlays import it instead of androidx.core's. EMAIL_ADDRESS is the same expression as androidx.core.
package ai.littlepress.a2ui.compat.androidx.core.util

public object PatternsCompat {
    public val EMAIL_ADDRESS: Pattern =
        Pattern(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                "\\@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(" +
                "\\." +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
                ")+"
        )

    /** The `matcher(input).matches()` shape of java.util.regex.Pattern that upstream calls. */
    public class Pattern(expression: String) {
        private val regex = Regex(expression)

        public fun matcher(input: CharSequence): Matcher = Matcher(regex, input)
    }

    public class Matcher(private val regex: Regex, private val input: CharSequence) {
        public fun matches(): Boolean = regex.matches(input)
    }
}
