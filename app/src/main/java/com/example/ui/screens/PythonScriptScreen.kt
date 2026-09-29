package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ComplexityLevel
import com.example.data.repository.PythonScriptGenerator
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ComplexityChip
import com.example.ui.theme.TerminalBackground
import com.example.ui.theme.TerminalComment
import com.example.ui.theme.TerminalText
import com.example.ui.viewmodel.BenchmarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PythonScriptScreen(
    viewModel: BenchmarkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pythonScript by viewModel.pythonScript.collectAsState()
    val pythonConfig by viewModel.pythonConfig.collectAsState()
    val consoleLogs by viewModel.pythonConsoleLogs.collectAsState()
    val isPythonRunning by viewModel.isPythonRunning.collectAsState()
    val selectedModel by viewModel.selectedModel.collectAsState()

    var activeTab by remember { mutableStateOf(0) } // 0: Python Script, 1: Execution Sandbox Console, 2: Settings
    var showModelDropdown by remember { mutableStateOf(false) }

    val availableModels = listOf("gemini-3.5-flash", "gemini-3.1-pro-preview")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Python Benchmark Studio",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Export and execute Python evaluation scripts to measure accuracy & latency",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick actions
            Row {
                IconButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("evaluate_prompts.py", pythonScript)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "evaluate_prompts.py copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_python_script_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy script", tint = MaterialTheme.colorScheme.primary)
                }

                IconButton(
                    onClick = {
                        val sendIntent: Intent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, pythonScript)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Export evaluate_prompts.py")
                        context.startActivity(shareIntent)
                    }
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = "Share script", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        // Navigation Tabs
        TabRow(
            selectedTabIndex = activeTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeTab == 0,
                onClick = { activeTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Script (evaluate_prompts.py)", fontSize = 12.sp, fontWeight = if (activeTab == 0) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = activeTab == 1,
                onClick = { activeTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sandbox Console", fontSize = 12.sp, fontWeight = if (activeTab == 1) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            )
            Tab(
                selected = activeTab == 2,
                onClick = { activeTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Config", fontSize = 12.sp, fontWeight = if (activeTab == 2) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            )
        }

        when (activeTab) {
            0 -> {
                // Script View Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(
                                    text = "Ready to Run Standalone Script",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Uses requests, pandas, and matplotlib to evaluate accuracy & latency",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = {
                                    activeTab = 1
                                    viewModel.simulatePythonExecution()
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Run Sandbox")
                            }
                        }
                    }

                    CodeBlockView(
                        code = pythonScript,
                        title = "evaluate_prompts.py",
                        language = "python",
                        collapsible = false
                    )
                }
            }

            1 -> {
                // Interactive Console Sandbox Tab
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.simulatePythonExecution() },
                            enabled = !isPythonRunning,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("run_python_sandbox_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPythonRunning) "Running Script..." else "Run Python Benchmark")
                        }

                        if (consoleLogs.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { viewModel.simulatePythonExecution() },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Re-run")
                            }
                        }
                    }

                    if (isPythonRunning) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                    }

                    // Terminal Output Window
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = TerminalBackground),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (consoleLogs.isEmpty()) {
                                Text(
                                    text = "# Click 'Run Python Benchmark' to execute the script in the sandbox terminal...\n# It will measure accuracy, latency across 0-shot, few-shot, and structured strategies.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = TerminalComment,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                            } else {
                                consoleLogs.forEach { line ->
                                    val color = when {
                                        line.startsWith("🚀") || line.startsWith("📌") -> Color(0xFF58A6FF)
                                        line.startsWith("🧪") -> Color(0xFFFFA657)
                                        line.contains("STRUCTURED") -> Color(0xFF3FB950)
                                        line.contains("FEW_SHOT") -> Color(0xFFD29922)
                                        line.contains("ZERO_SHOT") -> Color(0xFF79C0FF)
                                        line.startsWith("=") || line.startsWith("-") -> Color(0xFF8B949E)
                                        line.startsWith("🏆") || line.startsWith("📈") || line.startsWith("✅") -> Color(0xFF3FB950)
                                        else -> TerminalText
                                    }

                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        color = color,
                                        fontSize = 11.5.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Script Configuration Tab
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Python Evaluation Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Model Selector
                            ExposedDropdownMenuBox(
                                expanded = showModelDropdown,
                                onExpandedChange = { showModelDropdown = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedModel,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Target LLM Model") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showModelDropdown) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = showModelDropdown,
                                    onDismissRequest = { showModelDropdown = false }
                                ) {
                                    availableModels.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m) },
                                            onClick = {
                                                viewModel.setModel(m)
                                                showModelDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Temperature slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sampling Temperature", style = MaterialTheme.typography.labelMedium)
                                    Text("${pythonConfig.temperature}", fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = pythonConfig.temperature,
                                    onValueChange = {
                                        viewModel.updatePythonConfig(pythonConfig.copy(temperature = (it * 10).toInt() / 10f))
                                    },
                                    valueRange = 0.0f..1.0f,
                                    steps = 9
                                )
                            }

                            // Trials per prompt
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Evaluation Trials per Prompt", style = MaterialTheme.typography.labelMedium)
                                    Text("${pythonConfig.trialsPerPrompt} trials", fontWeight = FontWeight.Bold)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(1, 3, 5).forEach { count ->
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updatePythonConfig(pythonConfig.copy(trialsPerPrompt = count))
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = if (pythonConfig.trialsPerPrompt == count) ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else ButtonDefaults.outlinedButtonColors()
                                        ) {
                                            Text("$count ${if (count == 1) "Trial" else "Trials"}")
                                        }
                                    }
                                }
                            }

                            // Plotting toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Include Matplotlib Chart Code", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    Text("Generates comparative bar chart PNG on script completion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = pythonConfig.includePlotting,
                                    onCheckedChange = {
                                        viewModel.updatePythonConfig(pythonConfig.copy(includePlotting = it))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
