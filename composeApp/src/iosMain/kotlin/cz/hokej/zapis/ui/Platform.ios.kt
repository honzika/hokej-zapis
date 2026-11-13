package cz.hokej.zapis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import cz.hokej.zapis.model.Logo
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOn() {
    DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose { UIApplication.sharedApplication.idleTimerDisabled = false }
    }
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) = Unit

// Google Cast SDK pro iOS se doplní později.
@Composable
actual fun CastButton() = Unit

// Výběr fotky (PHPicker) se doplní s iOS buildem v Xcode; zobrazení loga funguje.
@Composable
actual fun rememberLogoPicker(onPicked: (Logo) -> Unit): (() -> Unit)? = null

actual fun decodeLogo(logo: Logo): ImageBitmap? =
    runCatching { org.jetbrains.skia.Image.makeFromEncoded(logo.bytes).toComposeImageBitmap() }.getOrNull()
