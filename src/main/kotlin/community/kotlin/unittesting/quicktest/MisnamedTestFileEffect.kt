package community.kotlin.unittesting.quicktest

import kotlinx.algebraiceffects.NotificationEffect

/** Notification emitted when a file named `tests.kts` is found. */
class MisnamedTestFileEffect(val path: String) :
    NotificationEffect("warning: ignoring file named 'tests.kts' (expected 'test.kts'): $path", null)
