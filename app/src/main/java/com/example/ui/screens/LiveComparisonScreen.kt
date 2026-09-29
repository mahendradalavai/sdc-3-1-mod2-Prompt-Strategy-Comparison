package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BenchmarkTestCase
import com.example.data.model.ComplexityLevel
import com.example.data.model.PromptStrategy
import com.example.ui.components.CodeBlockView
import com.example.ui.components.ComplexityChip
import com.example.ui.components.StrategyBadge
import com.example.ui.components.StrategyResultInspectorCard
import com.example.ui.viewmodel.BenchmarkViewModel

@Composable
fun LiveComparisonScreen(
    viewModel: BenchmarkViewModel,
    modifier: Modifier = Modifier
) {
    val testCases by viewModel.presetTestCases.collectAsState()
    val selectedTestCase by viewModel.selectedTestCase.collectAsState()
    val currentRun by viewModel.currentComparisonRun.collectAsState()
    val isEvaluating by viewModel.isEvaluating.collectAsState()
    val evalProgress by viewModel.evaluationProgress.collectAsState()
    val evalStatus by viewModel.evaluationStatusMessage.collectAsState()

    var showCustomDialog by remember { mutableStateOf(false) }
    var selectedStrategyTab by remember { mutableStateOf(0) } // 0: All, 1: 0-Shot, 2: Few-Shot, 3: Structured

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Test Case Selector Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Prompt Evaluation Playground",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Select a task and compare strategy outputs in real-time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showCustomDialog = true },
                modifier = Modifier.testTag("add_custom_prompt_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom prompt", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Horizontal Test Case Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            testCases.forEach { tc ->
                ComplexityChip(
                    complexity = tc.complexity,
                    isSelected = selectedTestCase.id == tc.id,
                    onClick = {
                        viewModel.selectTestCase(tc)
                    }
                )
            }
        }

        // Selected Task Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("selected_task_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedTestCase.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    ComplexityChip(complexity = selectedTestCase.complexity)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = selectedTestCase.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Task Input Data:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                CodeBlockView(
                    code = selectedTestCase.inputData,
                    title = "Input",
                    language = "text",
                    collapsible = true,
                    defaultExpanded = false
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Run Evaluation Button
                Button(
                    onClick = { viewModel.runSingleComparison(selectedTestCase) },
                    enabled = !isEvaluating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_comparison_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isEvaluating) "Evaluating 3 Strategies..." else "Run 3-Way Strategy Comparison")
                }

                if (isEvaluating) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = evalStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { evalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                }
            }
        }

        // Strategy Comparison Results
        if (currentRun != null) {
            val run = currentRun!!

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comparative Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Quick summary latency diff
                Text(
                    text = "Fastest: 0-Shot (${run.zeroShotResult.latencyMs}ms)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Strategy filter tabs
            TabRow(
                selectedTabIndex = selectedStrategyTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .then(Modifier)
            ) {
                Tab(
                    selected = selectedStrategyTab == 0,
                    onClick = { selectedStrategyTab = 0 },
                    text = { Text("Side-by-Side", fontSize = 12.sp, fontWeight = if (selectedStrategyTab == 0) FontWeight.SemiBold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedStrategyTab == 1,
                    onClick = { selectedStrategyTab = 1 },
                    text = { Text("Zero-Shot", fontSize = 12.sp, fontWeight = if (selectedStrategyTab == 1) FontWeight.SemiBold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedStrategyTab == 2,
                    onClick = { selectedStrategyTab = 2 },
                    text = { Text("Few-Shot", fontSize = 12.sp, fontWeight = if (selectedStrategyTab == 2) FontWeight.SemiBold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedStrategyTab == 3,
                    onClick = { selectedStrategyTab = 3 },
                    text = { Text("Structured", fontSize = 12.sp, fontWeight = if (selectedStrategyTab == 3) FontWeight.SemiBold else FontWeight.Normal) }
                )
            }

            when (selectedStrategyTab) {
                0 -> {
                    // Show all 3 results in sequence
                    StrategyResultInspectorCard(result = run.zeroShotResult)
                    StrategyResultInspectorCard(result = run.fewShotResult)
                    StrategyResultInspectorCard(result = run.structuredResult)
                }
                1 -> StrategyResultInspectorCard(result = run.zeroShotResult)
                2 -> StrategyResultInspectorCard(result = run.fewShotResult)
                3 -> StrategyResultInspectorCard(result = run.structuredResult)
            }

            // Expected Ground Truth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Target Ground Truth Reference",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlockView(
                        code = selectedTestCase.expectedOutput,
                        title = "Ground Truth",
                        language = "json",
                        collapsible = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Custom Prompt Dialog
    if (showCustomDialog) {
        CustomPromptDialog(
            onDismiss = { showCustomDialog = false },
            onSave = { title, desc, comp, input, zShot, fShot, sShot, schema, expected ->
                viewModel.addCustomTestCase(title, desc, comp, input, zShot, fShot, sShot, schema, expected)
                showCustomDialog = false
            }
        )
    }
}

@Composable
fun CustomPromptDialog(
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        desc: String,
        comp: ComplexityLevel,
        input: String,
        zShot: String,
        fShot: String,
        sShot: String,
        schema: String,
        expected: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var inputData by remember { mutableStateOf("") }
    var complexity by remember { mutableStateOf(ComplexityLevel.MEDIUM) }
    var zeroShotPrompt by remember { mutableStateOf("") }
    var fewShotPrompt by remember { mutableStateOf("") }
    var structuredPrompt by remember { mutableStateOf("") }
    var jsonSchema by remember { mutableStateOf("{\n  \"type\": \"object\",\n  \"properties\": {}\n}") }
    var expectedOutput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Custom Prompt Benchmark", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Contract Expiration Parser") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Complexity picker
                Text("Complexity Level:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ComplexityLevel.values().forEach { c ->
                        ComplexityChip(
                            complexity = c,
                            isSelected = complexity == c,
                            onClick = { complexity = c }
                        )
                    }
                }

                OutlinedTextField(
                    value = inputData,
                    onValueChange = { inputData = it },
                    label = { Text("Input Text / Snippet") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = zeroShotPrompt,
                    onValueChange = { zeroShotPrompt = it },
                    label = { Text("Zero-Shot Prompt") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = fewShotPrompt,
                    onValueChange = { fewShotPrompt = it },
                    label = { Text("Few-Shot Prompt (with Examples)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                OutlinedTextField(
                    value = structuredPrompt,
                    onValueChange = { structuredPrompt = it },
                    label = { Text("Structured Prompt (Schema & CoT)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = expectedOutput,
                    onValueChange = { expectedOutput = it },
                    label = { Text("Expected Ground Truth") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title,
                        "Custom user-defined prompt strategy comparison",
                        complexity,
                        inputData,
                        zeroShotPrompt.ifBlank { "Process this input: $inputData" },
                        fewShotPrompt.ifBlank { "Example 1:\nInput: Test\nOutput: {}\nNow process:\n$inputData" },
                        structuredPrompt.ifBlank { "Analyze and return structured JSON for: $inputData" },
                        jsonSchema,
                        expectedOutput
                    )
                }
            ) {
                Text("Save Benchmark")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
