package community.kotlin.unittesting.quicktest

import kotlinx.algebraiceffects.NotificationEffect

/** Notification emitted when a test script lacks a package declaration at the top. */
class PackageWarningEffect(val path: String) :
    NotificationEffect("warning: no package at top of file: $path", null)
