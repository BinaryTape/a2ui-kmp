package org.robolectric.annotation

/** Accepts the upstream `@Config(minSdk = ...)`; nothing reads it here. */
annotation class Config(val minSdk: Int = -1)
