package com.example.snapbadgers

import android.view.KeyEvent
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Guarantees the device screen is on and unlocked before Compose tests inject input.
 *
 * Why this matters: on WSL+usbipd rigs the phone can drop to the lock screen mid-run,
 * and Compose `performClick` / `performTextInput` then fail with
 * `AssertionError: Failed to inject touch input`. Calling this in @Before codifies the
 * "Stay awake while charging + KEYCODE_WAKEUP" workaround.
 */
fun wakeScreenForTest() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_WAKEUP)
    instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_MENU) // dismiss swipe-up lock on unlocked devices
    instrumentation.waitForIdleSync()
}
