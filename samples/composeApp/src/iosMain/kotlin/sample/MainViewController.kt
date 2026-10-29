package sample

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Called from Swift (iosApp/iosApp/ContentView.swift), with the tab to open first. */
fun MainViewController(initialTab: String): UIViewController = ComposeUIViewController { App(initialTab) }
