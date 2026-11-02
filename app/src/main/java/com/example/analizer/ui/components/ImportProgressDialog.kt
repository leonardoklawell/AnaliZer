package com.example.analizer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.analizer.ui.viewmodel.ImportProgressState

@Composable
fun ImportProgressDialog(
    progressState: ImportProgressState
) {
    if (!progressState.isImporting) return

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0B1825)
            ),
            border = BorderStroke(1.dp, Color(0xFFEBA036))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = Color(0xFFEBA036),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = progressState.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = progressState.stepText,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFEBA036),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(progressState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressState.progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = Color(0xFFEBA036),
                        trackColor = Color(0xFF02121C)
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF02121C))
                ) {
                    Text(
                        text = progressState.stageMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFD0DCE5),
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Text(
                    text = "Dados e configurações são salvos e mantidos automaticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8C9BAE)
                )
            }
        }
    }
}
