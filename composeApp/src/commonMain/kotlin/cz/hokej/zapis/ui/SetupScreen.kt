package cz.hokej.zapis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.hokej.zapis.model.MatchConfig
import cz.hokej.zapis.model.Team
import cz.hokej.zapis.model.TeamColors

@Composable
fun SetupScreen(initial: MatchConfig, onStart: (MatchConfig) -> Unit) {
    var homeName by remember { mutableStateOf(initial.home.name) }
    var homeColor by remember { mutableLongStateOf(initial.home.color) }
    var awayName by remember { mutableStateOf(initial.away.name) }
    var awayColor by remember { mutableLongStateOf(initial.away.color) }
    var minutes by remember { mutableIntStateOf(initial.periodLengthSec / 60) }

    Column(
        Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Nový zápas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

        TeamEditor("Domácí", homeName, { homeName = it }, homeColor, { homeColor = it })
        TeamEditor("Hosté", awayName, { awayName = it }, awayColor, { awayColor = it })

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Délka třetiny", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalIconButton(onClick = { minutes = (minutes - 1).coerceAtLeast(1) }) {
                        Icon(Icons.Default.Remove, "Ubrat minutu")
                    }
                    Text(
                        "$minutes min",
                        Modifier.width(110.dp).padding(horizontal = 8.dp),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    FilledTonalIconButton(onClick = { minutes = (minutes + 1).coerceAtMost(20) }) {
                        Icon(Icons.Default.Add, "Přidat minutu")
                    }
                }
                Text("3 třetiny", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                onStart(
                    MatchConfig(
                        home = Team(homeName.trim().ifEmpty { "Domácí" }, homeColor),
                        away = Team(awayName.trim().ifEmpty { "Hosté" }, awayColor),
                        periodLengthSec = minutes * 60,
                    )
                )
            },
            Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Začít zápas", fontSize = 18.sp) }
    }
}

@Composable
private fun TeamEditor(
    label: String,
    name: String,
    onName: (String) -> Unit,
    color: Long,
    onColor: (Long) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { onName(it.take(24)) },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TeamColors.forEach { c ->
                    val selected = c == color
                    Spacer(
                        Modifier.size(36.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (selected) 3.dp else 1.dp, if (selected) Color.White else Color.White.copy(alpha = 0.25f), CircleShape)
                            .clickable { onColor(c) }
                    )
                }
            }
        }
    }
}
