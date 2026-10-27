package cz.hokej.zapis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hokej.zapis.model.MatchState
import cz.hokej.zapis.model.Side
import cz.hokej.zapis.model.formatSec

@Composable
fun SummaryScreen(state: MatchState, onNewMatch: () -> Unit) {
    val home = state.config.home
    val away = state.config.away
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Konec zápasu", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(home.name, Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.score(Side.HOME)} : ${state.score(Side.AWAY)}",
                Modifier.padding(horizontal = 16.dp),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
            )
            Text(away.name, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
        }
        val byPeriod = (1..state.config.periods).joinToString(", ") { p ->
            "${state.goals.count { it.period == p && it.side == Side.HOME }}:${state.goals.count { it.period == p && it.side == Side.AWAY }}"
        }
        Text("Třetiny ($byPeriod)", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text("Góly", style = MaterialTheme.typography.titleMedium)
        if (state.goals.isEmpty()) Text("Žádné góly", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.goals.forEach { g ->
            val team = state.config.team(g.side)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(RoundedCornerShape(6.dp)).background(Color(team.color)))
                Spacer(Modifier.width(10.dp))
                Text("${g.period}. tř. ${formatSec(g.atSec)}", Modifier.width(100.dp), style = TabularNums, maxLines = 1)
                Text(listOfNotNull(team.name, g.scorer).joinToString(" – "))
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onNewMatch, Modifier.fillMaxWidth().height(56.dp)) { Text("Nový zápas", fontSize = 18.sp) }
    }
}
