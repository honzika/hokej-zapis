package cz.hokej.zapis

import androidx.compose.ui.window.ComposeUIViewController
import cz.hokej.zapis.history.FileTextStore
import cz.hokej.zapis.ui.App

fun MainViewController() = ComposeUIViewController { App(history = FileTextStore.history) }
