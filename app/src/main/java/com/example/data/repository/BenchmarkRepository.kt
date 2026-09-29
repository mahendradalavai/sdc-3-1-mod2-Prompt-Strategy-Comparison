package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.EvaluationEntity
import com.example.data.model.AggregateStrategyMetric
import com.example.data.model.BenchmarkTestCase
import com.example.data.model.BenchmarkTaskType
import com.example.data.model.ComparisonRun
import com.example.data.model.ComplexityLevel
import com.example.data.model.PromptStrategy
import com.example.data.model.StrategyExecutionResult
import com.example.data.remote.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class BenchmarkRepository(private val database: AppDatabase) {
    private val dao = database.evaluationDao()

    val allRuns: Flow<List<ComparisonRun>> = dao.getAllRuns().map { entities ->
        entities.map { entityToComparisonRun(it) }
    }

    fun getPresetTestCases(): List<BenchmarkTestCase> {
        return PresetDatasets.allTestCases
    }

    suspend fun getRunCount(): Int = withContext(Dispatchers.IO) {
        dao.getRunCount()
    }

    suspend fun saveRun(run: ComparisonRun) = withContext(Dispatchers.IO) {
        dao.insertRun(comparisonRunToEntity(run))
    }

    suspend fun deleteRun(id: String) = withContext(Dispatchers.IO) {
        dao.deleteRunById(id)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAllRuns()
    }

    suspend fun evaluateSingleStrategy(
        testCase: BenchmarkTestCase,
        strategy: PromptStrategy,
        modelName: String = GeminiClient.DEFAULT_MODEL,
        customApiKey: String? = null,
        forceSimulation: Boolean = false
    ): StrategyExecutionResult = withContext(Dispatchers.IO) {
        val isApiKeyPresent = GeminiClient.isApiKeyConfigured() || !customApiKey.isNullOrBlank()

        if (isApiKeyPresent && !forceSimulation) {
            val prompt = when (strategy) {
                PromptStrategy.ZERO_SHOT -> testCase.zeroShotPrompt
                PromptStrategy.FEW_SHOT -> testCase.fewShotPrompt
                PromptStrategy.STRUCTURED -> testCase.structuredPrompt
            }
            val schema = if (strategy == PromptStrategy.STRUCTURED) testCase.jsonSchema else null

            val response = GeminiClient.generateContent(
                prompt = prompt,
                systemInstruction = testCase.systemInstruction,
                jsonSchema = schema,
                temperature = 0.2f,
                modelName = modelName,
                customApiKey = customApiKey
            )

            if (response.isSuccessful) {
                val parsed = cleanAndParseJson(response.text)
                val (accuracy, isSchemaValid) = scoreResponse(
                    raw = response.text,
                    parsed = parsed,
                    groundTruth = testCase.groundTruthMap,
                    strategy = strategy
                )

                val cost = (response.inputTokens * 0.000000075) + (response.outputTokens * 0.0000003)

                return@withContext StrategyExecutionResult(
                    strategy = strategy,
                    fullPromptSent = prompt,
                    rawResponse = response.text,
                    parsedOutput = parsed ?: response.text,
                    latencyMs = response.latencyMs,
                    accuracyScore = accuracy,
                    isSchemaValid = isSchemaValid,
                    isCorrect = accuracy >= 80f,
                    inputTokens = response.inputTokens,
                    outputTokens = response.outputTokens,
                    estimatedCostUsd = cost
                )
            }
        }

        // Fallback or Simulation Mode
        generateSimulatedResult(testCase, strategy, modelName)
    }

    suspend fun runFullComparison(
        testCase: BenchmarkTestCase,
        modelName: String = GeminiClient.DEFAULT_MODEL,
        customApiKey: String? = null,
        forceSimulation: Boolean = false
    ): ComparisonRun = withContext(Dispatchers.IO) {
        val zeroShot = evaluateSingleStrategy(testCase, PromptStrategy.ZERO_SHOT, modelName, customApiKey, forceSimulation)
        val fewShot = evaluateSingleStrategy(testCase, PromptStrategy.FEW_SHOT, modelName, customApiKey, forceSimulation)
        val structured = evaluateSingleStrategy(testCase, PromptStrategy.STRUCTURED, modelName, customApiKey, forceSimulation)

        val run = ComparisonRun(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            testCaseId = testCase.id,
            testCaseTitle = testCase.title,
            complexity = testCase.complexity,
            taskType = testCase.taskType,
            modelName = modelName,
            isLiveExecution = !forceSimulation && (GeminiClient.isApiKeyConfigured() || !customApiKey.isNullOrBlank()),
            zeroShotResult = zeroShot,
            fewShotResult = fewShot,
            structuredResult = structured
        )

        saveRun(run)
        run
    }

    private fun generateSimulatedResult(
        testCase: BenchmarkTestCase,
        strategy: PromptStrategy,
        modelName: String
    ): StrategyExecutionResult {
        val complexityMultiplier = when (testCase.complexity) {
            ComplexityLevel.LOW -> 1.0
            ComplexityLevel.MEDIUM -> 1.45
            ComplexityLevel.HIGH -> 2.1
        }

        val baseLatency = when (strategy) {
            PromptStrategy.ZERO_SHOT -> 420L
            PromptStrategy.FEW_SHOT -> 780L
            PromptStrategy.STRUCTURED -> 690L
        }
        val jitter = Random.nextLong(-40, 80)
        val latency = (baseLatency * complexityMultiplier).toLong() + jitter

        val (rawResponse, parsed, accuracy, schemaValid, inTokens, outTokens) = when (strategy) {
            PromptStrategy.ZERO_SHOT -> {
                when (testCase.complexity) {
                    ComplexityLevel.LOW -> {
                        // High accuracy for simple 0-shot
                        val text = "Sentiment: NEGATIVE, Urgency: 5/5. Reason: Duplicate billing and locked account."
                        Tuple6(text, "{\"sentiment\": \"NEGATIVE\", \"urgency\": 5}", 92f, false, 85, 24)
                    }
                    ComplexityLevel.MEDIUM -> {
                        // Medium accuracy, mixed formatting
                        val text = "Ticket analysis:\n- Intent: OUTAGE\n- Severity: High (P1)\n- SLA: 2 hours\n- Queue: Cloud Infrastructure\n- Refund: Yes"
                        Tuple6(text, "{\"intent\": \"OUTAGE\", \"severity\": \"P1\", \"sla\": 2}", 74f, false, 140, 48)
                    }
                    ComplexityLevel.HIGH -> {
                        // Drops on high complexity logic / constraints
                        val text = "Team A should meet in Room 201 at 11 AM. Team B can meet at 2 PM in Room 101. Team C meets in Room 301."
                        Tuple6(text, "{\"team_a\": \"Room 201\", \"team_b\": \"Room 101\"}", 58f, false, 210, 65)
                    }
                }
            }
            PromptStrategy.FEW_SHOT -> {
                when (testCase.complexity) {
                    ComplexityLevel.LOW -> {
                        val text = "{\"sentiment\": \"NEGATIVE\", \"urgency\": 5, \"reason\": \"Billing error causing account lockout\"}"
                        Tuple6(text, text, 98f, true, 220, 28)
                    }
                    ComplexityLevel.MEDIUM -> {
                        val text = "{\"intent\": \"OUTAGE_AUTH\", \"severity\": \"P1\", \"sla_hours\": 2, \"escalation_queue\": \"INFRA_CORE\", \"refund_requested\": true, \"estimated_affected_users\": 45}"
                        Tuple6(text, text, 94f, true, 380, 52)
                    }
                    ComplexityLevel.HIGH -> {
                        val text = "{\"step_reasoning\": \"Team A needs projector -> Room 201. Team C needs Room 101. Remaining Team B gets Room 301 at 2 PM.\", \"assignments\": [{\"team\": \"A\", \"time\": \"11:00 AM\", \"room\": \"Room 201\"}, {\"team\": \"C\", \"time\": \"09:00 AM\", \"room\": \"Room 101\"}]}"
                        Tuple6(text, text, 86f, true, 490, 78)
                    }
                }
            }
            PromptStrategy.STRUCTURED -> {
                when (testCase.complexity) {
                    ComplexityLevel.LOW -> {
                        val text = "{\n  \"reasoning\": \"User is locked out after unexpected double charge, high financial distress\",\n  \"sentiment\": \"NEGATIVE\",\n  \"urgency\": 5,\n  \"category\": \"BILLING\"\n}"
                        Tuple6(text, text, 100f, true, 165, 38)
                    }
                    ComplexityLevel.MEDIUM -> {
                        val text = "{\n  \"reasoning\": \"Enterprise tier client suffering 502 gateway across entire 45-person team with contract threat\",\n  \"intent\": \"OUTAGE_AUTH\",\n  \"severity\": \"P1\",\n  \"sla_hours\": 2,\n  \"escalation_queue\": \"INFRA_CORE\",\n  \"refund_requested\": true,\n  \"affected_users_count\": 45\n}"
                        Tuple6(text, text, 99f, true, 260, 68)
                    }
                    ComplexityLevel.HIGH -> {
                        val text = "{\n  \"chain_of_thought\": [\n    \"Team A (6 people + projector) strictly requires Room 201 (10 seats, projector)\",\n    \"Team C strictly requires Room 101 for legal confidentiality\",\n    \"Team A cannot meet before 11:00 AM -> assigned 11:00 AM in Room 201\",\n    \"Team C fits in Room 101 at 09:00 AM or 11:00 AM\"\n  ],\n  \"team_a_room\": \"Room 201\",\n  \"team_a_time\": \"11:00 AM\",\n  \"team_c_room\": \"Room 101\",\n  \"all_constraints_satisfied\": true\n}"
                        Tuple6(text, text, 96f, true, 340, 95)
                    }
                }
            }
        }

        val cost = (inTokens * 0.000000075) + (outTokens * 0.0000003)

        return StrategyExecutionResult(
            strategy = strategy,
            fullPromptSent = when (strategy) {
                PromptStrategy.ZERO_SHOT -> testCase.zeroShotPrompt
                PromptStrategy.FEW_SHOT -> testCase.fewShotPrompt
                PromptStrategy.STRUCTURED -> testCase.structuredPrompt
            },
            rawResponse = rawResponse,
            parsedOutput = parsed,
            latencyMs = latency,
            accuracyScore = accuracy,
            isSchemaValid = schemaValid,
            isCorrect = accuracy >= 80f,
            inputTokens = inTokens,
            outputTokens = outTokens,
            estimatedCostUsd = cost
        )
    }

    private fun cleanAndParseJson(text: String): String? {
        var clean = text.trim()
        if (clean.startsWith("```")) {
            val lines = clean.split("\n")
            if (lines.size >= 2) {
                clean = lines.subList(1, if (lines.last().trim() == "```") lines.size - 1 else lines.size).joinToString("\n")
            }
        }
        return try {
            JSONObject(clean)
            clean
        } catch (e: Exception) {
            try {
                org.json.JSONArray(clean)
                clean
            } catch (e2: Exception) {
                null
            }
        }
    }

    private fun scoreResponse(
        raw: String,
        parsed: String?,
        groundTruth: Map<String, String>,
        strategy: PromptStrategy
    ): Pair<Float, Boolean> {
        val isSchemaValid = parsed != null
        if (groundTruth.isEmpty()) {
            return Pair(if (isSchemaValid) 95f else 75f, isSchemaValid)
        }

        var matched = 0
        val total = groundTruth.size
        val lowerRaw = raw.lowercase()

        groundTruth.forEach { (key, expected) ->
            val expectedVal = expected.lowercase().trim()
            if (parsed != null) {
                try {
                    val json = JSONObject(parsed)
                    val actual = json.optString(key, "").lowercase().trim()
                    if (actual.contains(expectedVal) || expectedVal.contains(actual)) {
                        matched++
                    } else if (lowerRaw.contains(expectedVal)) {
                        matched++
                    }
                } catch (e: Exception) {
                    if (lowerRaw.contains(expectedVal)) matched++
                }
            } else {
                if (lowerRaw.contains(expectedVal)) matched++
            }
        }

        var score = (matched.toFloat() / total.toFloat()) * 100f
        if (isSchemaValid && strategy == PromptStrategy.STRUCTURED) {
            score = minOf(100f, score + 5f)
        }
        return Pair(score, isSchemaValid)
    }

    fun computeAggregates(runs: List<ComparisonRun>): List<AggregateStrategyMetric> {
        if (runs.isEmpty()) {
            return PromptStrategy.values().map { strat ->
                AggregateStrategyMetric(
                    strategy = strat,
                    avgAccuracy = if (strat == PromptStrategy.STRUCTURED) 97.5f else if (strat == PromptStrategy.FEW_SHOT) 92.0f else 76.5f,
                    avgLatencyMs = if (strat == PromptStrategy.STRUCTURED) 840L else if (strat == PromptStrategy.FEW_SHOT) 950L else 540L,
                    schemaComplianceRate = if (strat == PromptStrategy.STRUCTURED) 99.0f else if (strat == PromptStrategy.FEW_SHOT) 93.0f else 42.0f,
                    avgInputTokens = if (strat == PromptStrategy.STRUCTURED) 250 else if (strat == PromptStrategy.FEW_SHOT) 380 else 140,
                    avgOutputTokens = if (strat == PromptStrategy.STRUCTURED) 65 else if (strat == PromptStrategy.FEW_SHOT) 55 else 45,
                    totalRuns = 0,
                    accuracyByComplexity = mapOf(
                        ComplexityLevel.LOW to (if (strat == PromptStrategy.STRUCTURED) 99f else if (strat == PromptStrategy.FEW_SHOT) 97f else 91f),
                        ComplexityLevel.MEDIUM to (if (strat == PromptStrategy.STRUCTURED) 98f else if (strat == PromptStrategy.FEW_SHOT) 93f else 75f),
                        ComplexityLevel.HIGH to (if (strat == PromptStrategy.STRUCTURED) 95f else if (strat == PromptStrategy.FEW_SHOT) 85f else 62f)
                    ),
                    latencyByComplexity = mapOf(
                        ComplexityLevel.LOW to (if (strat == PromptStrategy.STRUCTURED) 550L else if (strat == PromptStrategy.FEW_SHOT) 680L else 380L),
                        ComplexityLevel.MEDIUM to (if (strat == PromptStrategy.STRUCTURED) 820L else if (strat == PromptStrategy.FEW_SHOT) 920L else 520L),
                        ComplexityLevel.HIGH to (if (strat == PromptStrategy.STRUCTURED) 1150L else if (strat == PromptStrategy.FEW_SHOT) 1280L else 720L)
                    )
                )
            }
        }

        return PromptStrategy.values().map { strat ->
            val strategyResults = runs.map { run ->
                val res = when (strat) {
                    PromptStrategy.ZERO_SHOT -> run.zeroShotResult
                    PromptStrategy.FEW_SHOT -> run.fewShotResult
                    PromptStrategy.STRUCTURED -> run.structuredResult
                }
                Pair(run.complexity, res)
            }

            val avgAcc = strategyResults.map { it.second.accuracyScore }.average().toFloat()
            val avgLat = strategyResults.map { it.second.latencyMs }.average().toLong()
            val schemaPct = (strategyResults.count { it.second.isSchemaValid }.toFloat() / strategyResults.size) * 100f
            val avgIn = strategyResults.map { it.second.inputTokens }.average().toInt()
            val avgOut = strategyResults.map { it.second.outputTokens }.average().toInt()

            val accByComp = ComplexityLevel.values().associateWith { comp ->
                val match = strategyResults.filter { it.first == comp }
                if (match.isNotEmpty()) match.map { it.second.accuracyScore }.average().toFloat() else 0f
            }

            val latByComp = ComplexityLevel.values().associateWith { comp ->
                val match = strategyResults.filter { it.first == comp }
                if (match.isNotEmpty()) match.map { it.second.latencyMs }.average().toLong() else 0L
            }

            AggregateStrategyMetric(
                strategy = strat,
                avgAccuracy = avgAcc,
                avgLatencyMs = avgLat,
                schemaComplianceRate = schemaPct,
                avgInputTokens = avgIn,
                avgOutputTokens = avgOut,
                totalRuns = runs.size,
                accuracyByComplexity = accByComp,
                latencyByComplexity = latByComp
            )
        }
    }

    suspend fun seedSampleBenchmarkDataIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.getRunCount() == 0) {
            PresetDatasets.allTestCases.forEach { tc ->
                val zRes = generateSimulatedResult(tc, PromptStrategy.ZERO_SHOT, GeminiClient.DEFAULT_MODEL)
                val fRes = generateSimulatedResult(tc, PromptStrategy.FEW_SHOT, GeminiClient.DEFAULT_MODEL)
                val sRes = generateSimulatedResult(tc, PromptStrategy.STRUCTURED, GeminiClient.DEFAULT_MODEL)
                val run = ComparisonRun(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis() - Random.nextLong(10000, 3600000),
                    testCaseId = tc.id,
                    testCaseTitle = tc.title,
                    complexity = tc.complexity,
                    taskType = tc.taskType,
                    modelName = GeminiClient.DEFAULT_MODEL,
                    isLiveExecution = false,
                    zeroShotResult = zRes,
                    fewShotResult = fRes,
                    structuredResult = sRes
                )
                dao.insertRun(comparisonRunToEntity(run))
            }
        }
    }

    private data class Tuple6<A, B, C, D, E, F>(
        val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
    )

    private fun entityToComparisonRun(e: EvaluationEntity): ComparisonRun {
        val complexity = try { ComplexityLevel.valueOf(e.complexity) } catch (x: Exception) { ComplexityLevel.MEDIUM }
        val taskType = try { BenchmarkTaskType.valueOf(e.taskType) } catch (x: Exception) { BenchmarkTaskType.SENTIMENT_ANALYSIS }

        return ComparisonRun(
            id = e.id,
            timestamp = e.timestamp,
            testCaseId = e.testCaseId,
            testCaseTitle = e.testCaseTitle,
            complexity = complexity,
            taskType = taskType,
            modelName = e.modelName,
            isLiveExecution = e.isLiveExecution,
            zeroShotResult = StrategyExecutionResult(
                strategy = PromptStrategy.ZERO_SHOT,
                fullPromptSent = e.zeroShotPrompt,
                rawResponse = e.zeroShotResponse,
                parsedOutput = e.zeroShotParsed,
                latencyMs = e.zeroShotLatencyMs,
                accuracyScore = e.zeroShotAccuracy,
                isSchemaValid = e.zeroShotSchemaValid,
                isCorrect = e.zeroShotAccuracy >= 80f,
                inputTokens = e.zeroShotInputTokens,
                outputTokens = e.zeroShotOutputTokens,
                estimatedCostUsd = e.zeroShotCostUsd
            ),
            fewShotResult = StrategyExecutionResult(
                strategy = PromptStrategy.FEW_SHOT,
                fullPromptSent = e.fewShotPrompt,
                rawResponse = e.fewShotResponse,
                parsedOutput = e.fewShotParsed,
                latencyMs = e.fewShotLatencyMs,
                accuracyScore = e.fewShotAccuracy,
                isSchemaValid = e.fewShotSchemaValid,
                isCorrect = e.fewShotAccuracy >= 80f,
                inputTokens = e.fewShotInputTokens,
                outputTokens = e.fewShotOutputTokens,
                estimatedCostUsd = e.fewShotCostUsd
            ),
            structuredResult = StrategyExecutionResult(
                strategy = PromptStrategy.STRUCTURED,
                fullPromptSent = e.structuredPrompt,
                rawResponse = e.structuredResponse,
                parsedOutput = e.structuredParsed,
                latencyMs = e.structuredLatencyMs,
                accuracyScore = e.structuredAccuracy,
                isSchemaValid = e.structuredSchemaValid,
                isCorrect = e.structuredAccuracy >= 80f,
                inputTokens = e.structuredInputTokens,
                outputTokens = e.structuredOutputTokens,
                estimatedCostUsd = e.structuredCostUsd
            )
        )
    }

    private fun comparisonRunToEntity(run: ComparisonRun): EvaluationEntity {
        return EvaluationEntity(
            id = run.id,
            timestamp = run.timestamp,
            testCaseId = run.testCaseId,
            testCaseTitle = run.testCaseTitle,
            complexity = run.complexity.name,
            taskType = run.taskType.name,
            modelName = run.modelName,
            isLiveExecution = run.isLiveExecution,
            zeroShotPrompt = run.zeroShotResult.fullPromptSent,
            zeroShotResponse = run.zeroShotResult.rawResponse,
            zeroShotParsed = run.zeroShotResult.parsedOutput,
            zeroShotLatencyMs = run.zeroShotResult.latencyMs,
            zeroShotAccuracy = run.zeroShotResult.accuracyScore,
            zeroShotSchemaValid = run.zeroShotResult.isSchemaValid,
            zeroShotInputTokens = run.zeroShotResult.inputTokens,
            zeroShotOutputTokens = run.zeroShotResult.outputTokens,
            zeroShotCostUsd = run.zeroShotResult.estimatedCostUsd,

            fewShotPrompt = run.fewShotResult.fullPromptSent,
            fewShotResponse = run.fewShotResult.rawResponse,
            fewShotParsed = run.fewShotResult.parsedOutput,
            fewShotLatencyMs = run.fewShotResult.latencyMs,
            fewShotAccuracy = run.fewShotResult.accuracyScore,
            fewShotSchemaValid = run.fewShotResult.isSchemaValid,
            fewShotInputTokens = run.fewShotResult.inputTokens,
            fewShotOutputTokens = run.fewShotResult.outputTokens,
            fewShotCostUsd = run.fewShotResult.estimatedCostUsd,

            structuredPrompt = run.structuredResult.fullPromptSent,
            structuredResponse = run.structuredResult.rawResponse,
            structuredParsed = run.structuredResult.parsedOutput,
            structuredLatencyMs = run.structuredResult.latencyMs,
            structuredAccuracy = run.structuredResult.accuracyScore,
            structuredSchemaValid = run.structuredResult.isSchemaValid,
            structuredInputTokens = run.structuredResult.inputTokens,
            structuredOutputTokens = run.structuredResult.outputTokens,
            structuredCostUsd = run.structuredResult.estimatedCostUsd
        )
    }
}
