package cz.hokej.zapis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hokej.zapis.history.MatchRecord
import cz.hokej.zapis.history.TeamRecord
import cz.hokej.zapis.history.formatDateTime
import cz.hokej.zapis.model.Side

@Composable
fun HistoryScreen(
    matches: List<MatchRecord>,
    onOpen: (MatchRecord) -> Unit,
    onDelete: (MatchRecord) -> Unit,
    onBack: () -> Unit,
) {
    var toDelete by remember { mutableStateOf<MatchRecord?>(null) }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zpět") }
            Text("Zápasy", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Text("${matches.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 16.dp))
        }
        if (matches.isEmpty()) {
            Text(
                "Zatím žádné zápasy. Odehrané zápasy se tu objeví automaticky.",
                Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(matches, key = { it.id }) { m ->
                MatchCard(m, onClick = { onOpen(m) }, onDelete = { toDelete = m })
            }
        }
    }

    toDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Smazat zápas?") },
            text = { Text("${m.home.name} ${m.score(Side.HOME)}:${m.score(Side.AWAY)} ${m.away.name}, ${formatDateTime(m.startedAtMs)}") },
            confirmButton = { TextButton(onClick = { onDelete(m); toDelete = null }) { Text("Smazat") } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Zrušit") } },
        )
    }
}

@Composable
private fun MatchCard(m: MatchRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(start = 16.dp, top = 10.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${m.sport.label} · ${formatDateTime(m.startedAtMs)} · ${m.periods}× ${m.periodLengthSec / 60} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (!m.finished) {
                    Text(
                        "Nedokončený",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFB74D),
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Smazat zápas") }
            }
            Row(Modifier.padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                TeamName(m.home, Modifier.weight(1f), TextAlign.End)
                Text(
                    "${m.score(Side.HOME)} : ${m.score(Side.AWAY)}",
                    Modifier.padding(horizontal = 12.dp),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    style = TabularNums,
                )
                TeamName(m.away, Modifier.weight(1f), TextAlign.Start)
            }
            val byPeriod = (1..m.periods).joinToString(", ") { p ->
                "${m.goals.count { it.period == p && it.side == Side.HOME }}:${m.goals.count { it.period == p && it.side == Side.AWAY }}"
            }
            Text(
                "($byPeriod)",
                Modifier.fillMaxWidth().padding(end = 16.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TeamName(team: TeamRecord, modifier: Modifier, align: TextAlign) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start) {
        if (align == TextAlign.Start) ColorDot(team.color)
        Text(
            team.name,
            Modifier.padding(horizontal = 6.dp),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (align == TextAlign.End) ColorDot(team.color)
    }
}

@Composable
private fun ColorDot(color: Long) {
    Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(color)))
}
