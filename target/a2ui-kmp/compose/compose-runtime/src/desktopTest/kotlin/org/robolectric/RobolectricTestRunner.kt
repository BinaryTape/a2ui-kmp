package org.robolectric

import org.junit.runners.BlockJUnit4ClassRunner

/**
 * Two upstream test classes run with Robolectric only to get android.util.JsonReader on the JVM.
 * Here that reader is our common one, so a plain JUnit runner under the same name runs them.
 */
class RobolectricTestRunner(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass)
