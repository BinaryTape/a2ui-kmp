package androidx.test.filters

/** Test size; desktop runs every size. */
annotation class SmallTest

annotation class MediumTest

annotation class LargeTest

/** Limits a test to Android API levels; desktop has none, so it runs. */
annotation class SdkSuppress(
    val minSdkVersion: Int = 1,
    val maxSdkVersion: Int = Int.MAX_VALUE,
    val codeName: String = "unset",
)
