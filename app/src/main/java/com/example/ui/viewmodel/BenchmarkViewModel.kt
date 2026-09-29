package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AggregateStrategyMetric
import com.example.data.model.BenchmarkTaskType
import com.example.data.model.BenchmarkTestCase
import com.example.data.model.ComparisonRun
import com.example.data.model.ComplexityLevel
import com.example.data.model.FewShotExample
import com.example.data.model.PromptStrategy
import com.example.data.remote.GeminiClient
import com.example.data.repository.BenchmarkRepository
import com.example.data.repository.PythonScriptGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class BenchmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BenchmarkRepository(AppDatabase.getDatabase(application))

    val allRuns: StateFlow<List<ComparisonRun>> = repository.allRuns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _presetTestCases = MutableStateFlow(repository.getPresetTestCases())
    val presetTestCases: StateFlow<List<BenchmarkTestCase>> = _presetTestCases.asStateFlow()

    private val _selectedTestCase = MutableStateFlow(repository.getPresetTestCases().first())
    val selectedTestCase: StateFlow<BenchmarkTestCase> = _selectedTestCase.asStateFlow()

    private val _currentComparisonRun = MutableStateFlow<ComparisonRun?>(null)
    val currentComparisonRun: StateFlow<ComparisonRun?> = _currentComparisonRun.asStateFlow()

    private val _isEvaluating = MutableStateFlow(false)
    val isEvaluating: StateFlow<Boolean> = _isEvaluating.asStateFlow()

    private val _evaluationProgress = MutableStateFlow(0f)
    val evaluationProgress: StateFlow<Float> = _evaluationProgress.asStateFlow()

    private val _evaluationStatusMessage = MutableStateFlow("")
    val evaluationStatusMessage: StateFlow<String> = _evaluationStatusMessage.asStateFlow()

    // Model selection & API settings
    private val _selectedModel = MutableStateFlow(GeminiClient.DEFAULT_MODEL)
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _isLiveApiActive = MutableStateFlow(GeminiClient.isApiKeyConfigured())
    val isLiveApiActive: StateFlow<Boolean> = _isLiveApiActive.asStateFlow()

    // Python Studio state
    private val _pythonScript = MutableStateFlow("")
    val pythonScript: StateFlow<String> = _pythonScript.asStateFlow()

    private val _pythonConsoleLogs = MutableStateFlow<List<String>>(emptyList())
    val pythonConsoleLogs: StateFlow<List<String>> = _pythonConsoleLogs.asStateFlow()

    private val _isPythonRunning = MutableStateFlow(false)
    val isPythonRunning: StateFlow<Boolean> = _isPythonRunning.asStateFlow()

    private val _pythonConfig = MutableStateFlow(PythonScriptGenerator.PythonBenchmarkConfig())
    val pythonConfig: StateFlow<PythonScriptGenerator.PythonBenchmarkConfig> = _pythonConfig.asStateFlow()

    // Aggregate metrics for Dashboard
    val aggregateMetrics: StateFlow<List<AggregateStrategyMetric>> = MutableStateFlow(repository.computeAggregates(emptyList()))

    init {
        viewModelScope.launch {
            repository.seedSampleBenchmarkDataIfEmpty()
            updatePythonScript()
        }

        viewModelScope.launch {
            allRuns.collect { runs ->
                (aggregateMetrics as MutableStateFlow).value = repository.computeAggregates(runs)
                if (_currentComparisonRun.value == null && runs.isNotEmpty()) {
                    _currentComparisonRun.value = runs.first()
                }
            }
        }
    }

    fun selectTestCase(testCase: BenchmarkTestCase) {
        _selectedTestCase.value = testCase
    }

    fun setModel(model: String) {
        _selectedModel.value = model
        updatePythonScript()
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
        _isLiveApiActive.value = key.isNotBlank() || GeminiClient.isApiKeyConfigured()
        updatePythonScript()
    }

    fun updatePythonConfig(config: PythonScriptGenerator.PythonBenchmarkConfig) {
        _pythonConfig.value = config
        updatePythonScript()
    }

    private fun updatePythonScript() {
        _pythonScript.value = PythonScriptGenerator.generatePythonScript(
            testCases = _presetTestCases.value,
            config = _pythonConfig.value.copy(model = _selectedModel.value)
        )
    }

    fun runSingleComparison(testCase: BenchmarkTestCase = _selectedTestCase.value) {
        viewModelScope.launch {
            _isEvaluating.value = true
            _evaluationProgress.value = 0.1f
            _evaluationStatusMessage.value = "Preparing prompts for ${testCase.title}..."

            delay(200)
            _evaluationProgress.value = 0.35f
            _evaluationStatusMessage.value = "Evaluating Zero-Shot prompt..."

            val zeroShot = repository.evaluateSingleStrategy(
                testCase = testCase,
                strategy = PromptStrategy.ZERO_SHOT,
                modelName = _selectedModel.value,
                customApiKey = _customApiKey.value
            )

            _evaluationProgress.value = 0.65f
            _evaluationStatusMessage.value = "Evaluating Few-Shot prompt with exemplars..."

            val fewShot = repository.evaluateSingleStrategy(
                testCase = testCase,
                strategy = PromptStrategy.FEW_SHOT,
                modelName = _selectedModel.value,
                customApiKey = _customApiKey.value
            )

            _evaluationProgress.value = 0.90f
            _evaluationStatusMessage.value = "Evaluating Structured prompt with JSON Schema & CoT..."

            val structured = repository.evaluateSingleStrategy(
                testCase = testCase,
                strategy = PromptStrategy.STRUCTURED,
                modelName = _selectedModel.value,
                customApiKey = _customApiKey.value
            )

            val comparisonRun = ComparisonRun(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                testCaseId = testCase.id,
                testCaseTitle = testCase.title,
                complexity = testCase.complexity,
                taskType = testCase.taskType,
                modelName = _selectedModel.value,
                isLiveExecution = GeminiClient.isApiKeyConfigured() || _customApiKey.value.isNotBlank(),
                zeroShotResult = zeroShot,
                fewShotResult = fewShot,
                structuredResult = structured
            )

            repository.saveRun(comparisonRun)
            _currentComparisonRun.value = comparisonRun
            _evaluationProgress.value = 1f
            _evaluationStatusMessage.value = "Evaluation Complete!"
            delay(300)
            _isEvaluating.value = false
        }
    }

    fun runFullBatchBenchmark() {
        viewModelScope.launch {
            _isEvaluating.value = true
            val cases = _presetTestCases.value
            val totalSteps = cases.size

            for (i in cases.indices) {
                val tc = cases[i]
                _evaluationProgress.value = (i.toFloat() / totalSteps)
                _evaluationStatusMessage.value = "Benchmarking (${i + 1}/$totalSteps): ${tc.title}"
                repository.runFullComparison(
                    testCase = tc,
                    modelName = _selectedModel.value,
                    customApiKey = _customApiKey.value
                )
                delay(150)
            }

            _evaluationProgress.value = 1f
            _evaluationStatusMessage.value = "All ${cases.size} benchmarks evaluated and saved!"
            delay(400)
            _isEvaluating.value = false
        }
    }

    fun simulatePythonExecution() {
        viewModelScope.launch {
            _isPythonRunning.value = true
            val logs = mutableListOf<String>()
            _pythonConsoleLogs.value = emptyList()

            fun log(msg: String) {
                logs.add(msg)
                _pythonConsoleLogs.value = logs.toList()
            }

            log("$ python3 evaluate_prompts.py --model ${_selectedModel.value} --trials ${_pythonConfig.value.trialsPerPrompt}")
            delay(400)
            log("🚀 Starting Prompt Strategy Evaluation Benchmark...")
            log("📌 Target Model: ${_selectedModel.value} | Temperature: ${_pythonConfig.value.temperature}")
            log("================================================================================")
            delay(500)

            val cases = _presetTestCases.value.take(4)
            cases.forEach { tc ->
                log("\n🧪 Evaluating [${tc.complexity.name}] ${tc.title}...")
                delay(300)
                log("  • ZERO_SHOT    -> Acc: ${if (tc.complexity == ComplexityLevel.LOW) "92.0" else if (tc.complexity == ComplexityLevel.MEDIUM) "74.5" else "58.0"}% | Latency: ${if (tc.complexity == ComplexityLevel.LOW) "390" else if (tc.complexity == ComplexityLevel.MEDIUM) "540" else "730"}ms | Schema Valid: ${if (tc.complexity == ComplexityLevel.LOW) "66" else "0"}%")
                delay(300)
                log("  • FEW_SHOT     -> Acc: ${if (tc.complexity == ComplexityLevel.LOW) "98.0" else if (tc.complexity == ComplexityLevel.MEDIUM) "94.0" else "86.5"}% | Latency: ${if (tc.complexity == ComplexityLevel.LOW) "710" else if (tc.complexity == ComplexityLevel.MEDIUM) "920" else "1280"}ms | Schema Valid: 100%")
                delay(300)
                log("  • STRUCTURED   -> Acc: ${if (tc.complexity == ComplexityLevel.LOW) "100.0" else if (tc.complexity == ComplexityLevel.MEDIUM) "99.0" else "96.0"}% | Latency: ${if (tc.complexity == ComplexityLevel.LOW) "620" else if (tc.complexity == ComplexityLevel.MEDIUM) "840" else "1150"}ms | Schema Valid: 100%")
            }

            delay(600)
            log("\n================================================================================")
            log("📊 COMPARATIVE METRICS SUMMARY TABLE (pandas.DataFrame)")
            log("================================================================================")
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "Complexity", "Strategy", "Mean_Acc(%)", "Mean_Latency(ms)", "Avg_In_Tokens"))
            log("--------------------------------------------------------------------------------")
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "LOW", "ZERO_SHOT", "92.00", "390.4", "85"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "LOW", "FEW_SHOT", "98.00", "710.2", "220"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "LOW", "STRUCTURED", "100.00", "620.8", "165"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "MEDIUM", "ZERO_SHOT", "74.50", "540.1", "140"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "MEDIUM", "FEW_SHOT", "94.00", "920.6", "380"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "MEDIUM", "STRUCTURED", "99.00", "840.3", "260"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "HIGH", "ZERO_SHOT", "58.00", "730.5", "210"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "HIGH", "FEW_SHOT", "86.50", "1280.0", "490"))
            log(String.format("%-12s %-12s %-15s %-18s %-15s", "HIGH", "STRUCTURED", "96.00", "1150.2", "340"))

            delay(400)
            log("\n🏆 OVERALL STRATEGY RANKING:")
            log("1. STRUCTURED -> Mean Accuracy: 98.3% | Mean Latency: 870.4ms | Schema: 100%")
            log("2. FEW_SHOT   -> Mean Accuracy: 92.8% | Mean Latency: 970.2ms | Schema: 96%")
            log("3. ZERO_SHOT  -> Mean Accuracy: 74.8% | Mean Latency: 553.6ms | Schema: 22%")
            log("\n📈 Matplotlib chart generated -> 'prompt_strategy_comparison.png'")
            log("✅ Benchmark run finished successfully (Process exit code 0).")

            _isPythonRunning.value = false
        }
    }

    fun addCustomTestCase(
        title: String,
        description: String,
        complexity: ComplexityLevel,
        inputData: String,
        zeroShotPrompt: String,
        fewShotPrompt: String,
        structuredPrompt: String,
        jsonSchema: String,
        expectedOutput: String
    ) {
        val newCase = BenchmarkTestCase(
            id = "custom_${System.currentTimeMillis()}",
            title = title.ifBlank { "Custom Test Case" },
            description = description.ifBlank { "User created prompt comparison" },
            complexity = complexity,
            taskType = BenchmarkTaskType.CUSTOM,
            systemInstruction = "You are an intelligent assistant evaluated on prompt adherence and schema correctness.",
            inputData = inputData,
            zeroShotPrompt = zeroShotPrompt,
            fewShotExamples = emptyList(),
            fewShotPrompt = fewShotPrompt,
            structuredPrompt = structuredPrompt,
            jsonSchema = jsonSchema.ifBlank { "{\n  \"type\": \"object\"\n}" },
            expectedOutput = expectedOutput,
            groundTruthMap = mapOf("output" to expectedOutput)
        )

        _presetTestCases.value = listOf(newCase) + _presetTestCases.value
        _selectedTestCase.value = newCase
        updatePythonScript()
    }

    fun deleteRun(id: String) {
        viewModelScope.launch {
            repository.deleteRun(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }
}
