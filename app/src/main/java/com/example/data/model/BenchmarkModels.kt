package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FewShotColor
import com.example.ui.theme.HighComplexityColor
import com.example.ui.theme.LowComplexityColor
import com.example.ui.theme.MediumComplexityColor
import com.example.ui.theme.StructuredColor
import com.example.ui.theme.ZeroShotColor

enum class PromptStrategy(
    val title: String,
    val shortName: String,
    val description: String,
    val color: Color
) {
    ZERO_SHOT(
        title = "Zero-Shot",
        shortName = "0-Shot",
        description = "Direct instructions with no exemplars. Relies purely on pretrained model priors.",
        color = ZeroShotColor
    ),
    FEW_SHOT(
        title = "Few-Shot",
        shortName = "Few-Shot",
        description = "Instruction paired with 2-4 curated input/output examples establishing formatting patterns.",
        color = FewShotColor
    ),
    STRUCTURED(
        title = "Structured / CoT",
        shortName = "Structured",
        description = "Strict JSON schema enforcement with Chain-of-Thought (CoT) reasoning constraints.",
        color = StructuredColor
    )
}

enum class ComplexityLevel(
    val title: String,
    val description: String,
    val color: Color,
    val difficultyScore: Int // 1, 2, 3
) {
    LOW(
        title = "Low Complexity",
        description = "Single-attribute extraction, sentiment, simple classification",
        color = LowComplexityColor,
        difficultyScore = 1
    ),
    MEDIUM(
        title = "Medium Complexity",
        description = "Multi-field extraction, intent routing, structured customer tickets",
        color = MediumComplexityColor,
        difficultyScore = 2
    ),
    HIGH(
        title = "High Complexity",
        description = "Multi-step logical deduction, regulatory compliance matrix, code debugging",
        color = HighComplexityColor,
        difficultyScore = 3
    )
}

enum class BenchmarkTaskType(val label: String) {
    SENTIMENT_ANALYSIS("Sentiment Analysis"),
    ENTITY_EXTRACTION("Entity Extraction"),
    SUPPORT_TICKET_ROUTING("Support Intent & Routing"),
    FINANCIAL_AUDIT("Financial Categorization"),
    LOGICAL_REASONING("Multi-Step Logic"),
    CODE_DEBUGGING("Code Bug & Fix Synthesis"),
    COMPLIANCE_EXTRACTION("Compliance Policy Matrix"),
    CUSTOM("Custom Prompt")
}

data class FewShotExample(
    val input: String,
    val output: String
)

data class BenchmarkTestCase(
    val id: String,
    val title: String,
    val description: String,
    val complexity: ComplexityLevel,
    val taskType: BenchmarkTaskType,
    val systemInstruction: String,
    val inputData: String,
    val zeroShotPrompt: String,
    val fewShotExamples: List<FewShotExample>,
    val fewShotPrompt: String,
    val structuredPrompt: String,
    val jsonSchema: String,
    val expectedOutput: String,
    val validationRules: List<String> = emptyList(),
    val groundTruthMap: Map<String, String> = emptyMap()
)

data class StrategyExecutionResult(
    val strategy: PromptStrategy,
    val fullPromptSent: String,
    val rawResponse: String,
    val parsedOutput: String,
    val latencyMs: Long,
    val accuracyScore: Float, // 0 to 100
    val isSchemaValid: Boolean,
    val isCorrect: Boolean,
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCostUsd: Double,
    val reasoningChain: String? = null,
    val errorDetails: String? = null
)

data class ComparisonRun(
    val id: String,
    val timestamp: Long,
    val testCaseId: String,
    val testCaseTitle: String,
    val complexity: ComplexityLevel,
    val taskType: BenchmarkTaskType,
    val modelName: String,
    val isLiveExecution: Boolean,
    val zeroShotResult: StrategyExecutionResult,
    val fewShotResult: StrategyExecutionResult,
    val structuredResult: StrategyExecutionResult
)

data class AggregateStrategyMetric(
    val strategy: PromptStrategy,
    val avgAccuracy: Float,
    val avgLatencyMs: Long,
    val schemaComplianceRate: Float,
    val avgInputTokens: Int,
    val avgOutputTokens: Int,
    val totalRuns: Int,
    val accuracyByComplexity: Map<ComplexityLevel, Float>,
    val latencyByComplexity: Map<ComplexityLevel, Long>
)
