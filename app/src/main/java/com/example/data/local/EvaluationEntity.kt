package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.BenchmarkTaskType
import com.example.data.model.ComplexityLevel
import com.example.data.model.PromptStrategy
import com.example.data.model.StrategyExecutionResult

@Entity(tableName = "evaluation_runs")
data class EvaluationEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val testCaseId: String,
    val testCaseTitle: String,
    val complexity: String, // LOW, MEDIUM, HIGH
    val taskType: String,
    val modelName: String,
    val isLiveExecution: Boolean,

    // Zero-Shot Metrics
    val zeroShotPrompt: String,
    val zeroShotResponse: String,
    val zeroShotParsed: String,
    val zeroShotLatencyMs: Long,
    val zeroShotAccuracy: Float,
    val zeroShotSchemaValid: Boolean,
    val zeroShotInputTokens: Int,
    val zeroShotOutputTokens: Int,
    val zeroShotCostUsd: Double,

    // Few-Shot Metrics
    val fewShotPrompt: String,
    val fewShotResponse: String,
    val fewShotParsed: String,
    val fewShotLatencyMs: Long,
    val fewShotAccuracy: Float,
    val fewShotSchemaValid: Boolean,
    val fewShotInputTokens: Int,
    val fewShotOutputTokens: Int,
    val fewShotCostUsd: Double,

    // Structured Metrics
    val structuredPrompt: String,
    val structuredResponse: String,
    val structuredParsed: String,
    val structuredLatencyMs: Long,
    val structuredAccuracy: Float,
    val structuredSchemaValid: Boolean,
    val structuredInputTokens: Int,
    val structuredOutputTokens: Int,
    val structuredCostUsd: Double
)
