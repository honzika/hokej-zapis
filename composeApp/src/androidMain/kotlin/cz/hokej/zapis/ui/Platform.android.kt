package cz.hokej.zapis.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import cz.hokej.zapis.model.Logo
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

@Composable
actual fun rememberLogoPicker(onPicked: (Logo) -> Unit): (() -> Unit)? {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val callback by androidx.compose.runtime.rememberUpdatedState(onPicked)
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            val logo = withContext(Dispatchers.IO) { runCatching { loadLogo(context, uri) }.getOrNull() }
            if (logo != null) callback(logo)
        }
    }
    return {
        launcher.launch(
            androidx.activity.result.PickVisualMediaRequest(
                androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }
}

actual fun decodeLogo(logo: Logo): ImageBitmap? =
    BitmapFactory.decodeByteArray(logo.bytes, 0, logo.bytes.size)?.asImageBitmap()

private const val LOGO_MAX_PX = 160
private const val LOGO_MAX_BYTES = 16_000

/** Zmenší obrázek tak, aby se obě loga vešla do jedné Cast zprávy (limit 64 kB). WebP drží průhlednost. */
private fun loadLogo(context: Context, uri: Uri): Logo? {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= LOGO_MAX_PX) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val scale = minOf(1f, LOGO_MAX_PX.toFloat() / maxOf(decoded.width, decoded.height))
    val bitmap = Bitmap.createScaledBitmap(
        decoded,
        (decoded.width * scale).toInt().coerceAtLeast(1),
        (decoded.height * scale).toInt().coerceAtLeast(1),
        true,
    )
    val format = if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY
    else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
    var quality = 90
    while (true) {
        val bytes = ByteArrayOutputStream().also { bitmap.compress(format, quality, it) }.toByteArray()
        if (bytes.size <= LOGO_MAX_BYTES || quality <= 30) return Logo(bytes, "image/webp")
        quality -= 15
    }
}
