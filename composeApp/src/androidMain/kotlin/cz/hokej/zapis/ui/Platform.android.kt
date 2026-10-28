package cz.hokej.zapis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp

@Composable
actual fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
actual fun PlatformBackHandler(onBack: () -> Unit) = androidx.activity.compose.BackHandler(onBack = onBack)

@Composable
actual fun CastButton() {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            androidx.mediarouter.app.MediaRouteButton(ctx).apply {
                com.google.android.gms.cast.framework.CastButtonFactory.setUpMediaRouteButton(ctx.applicationContext, this)
                // Ukázat i bez nalezené TV, ať je jasné, kde se vysílání zapíná.
                setAlwaysVisible(true)
            }
        },
        modifier = androidx.compose.ui.Modifier.size(48.dp),
    )
}
