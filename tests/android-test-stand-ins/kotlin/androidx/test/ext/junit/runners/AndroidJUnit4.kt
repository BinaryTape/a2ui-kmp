// Desktop stand-ins for the Android test APIs that upstream's instrumented tests use, with the same
// names, so those test files compile unchanged and run on desktop with Compose Multiplatform's
// runComposeUiTest. Used only by desktopTest source sets; never published.
package androidx.test.ext.junit.runners

import org.junit.runners.BlockJUnit4ClassRunner

/** On desktop, AndroidJUnit4 is the plain JUnit 4 runner. */
class AndroidJUnit4(testClass: Class<*>) : BlockJUnit4ClassRunner(testClass)
