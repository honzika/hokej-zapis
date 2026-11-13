package cz.hokej.zapis.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.hokej.zapis.model.Logo

/** Otevře výběr obrázku; null = platforma výběr zatím neumí (iOS). */
@Composable
expect fun rememberLogoPicker(onPicked: (Logo) -> Unit): (() -> Unit)?

expect fun decodeLogo(logo: Logo): ImageBitmap?

/** Logo týmu, nebo nic (bez prázdného místa), když tým logo nemá. */
@Composable
fun TeamLogo(logo: Logo?, size: Dp, modifier: Modifier = Modifier) {
    val bitmap = remember(logo) { logo?.let(::decodeLogo) } ?: return
    Image(bitmap, contentDescription = null, modifier = modifier.size(size), contentScale = ContentScale.Fit)
}

/** Políčko v nastavení: klepnutím se vybere logo, křížkem odebere. */
@Composable
fun LogoSlot(logo: Logo?, onPick: (() -> Unit)?, onRemove: () -> Unit) {
    if (onPick == null && logo == null) return
    Box(Modifier.size(64.dp)) {
        val shape = RoundedCornerShape(12.dp)
        Box(
            Modifier.size(60.dp).align(Alignment.BottomStart)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .let { if (onPick != null) it.clickable(onClick = onPick) else it },
            contentAlignment = Alignment.Center,
        ) {
            if (logo != null) TeamLogo(logo, 52.dp)
            else Icon(Icons.Default.AddPhotoAlternate, "Vybrat logo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (logo != null) {
            Box(
                Modifier.size(24.dp).align(Alignment.TopEnd).offset(x = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onRemove)
                    .padding(4.dp),
            ) { Icon(Icons.Default.Close, "Odebrat logo") }
        }
    }
}
