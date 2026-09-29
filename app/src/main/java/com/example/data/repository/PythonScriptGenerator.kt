package com.example.data.repository

import com.example.data.model.BenchmarkTestCase
import com.example.data.model.ComplexityLevel
import com.example.data.model.PromptStrategy

object PythonScriptGenerator {

    data class PythonBenchmarkConfig(
        val model: String = "gemini-3.5-flash",
        val trialsPerPrompt: Int = 3,
        val temperature: Float = 0.2f,
        val includePlotting: Boolean = true,
        val filterComplexity: ComplexityLevel? = null
    )

    fun generatePythonScript(
        testCases: List<BenchmarkTestCase>,
        config: PythonBenchmarkConfig = PythonBenchmarkConfig()
    ): String {
        val filteredCases = if (config.filterComplexity != null) {
            testCases.filter { it.complexity == config.filterComplexity }
        } else {
            testCases
        }

        val testCasesJsonLiteral = buildString {
            append("TEST_CASES = [\n")
            filteredCases.forEach { tc ->
                append("    {\n")
                append("        \"id\": \"${tc.id}\",\n")
                append("        \"title\": \"${escapePythonString(tc.title)}\",\n")
                append("        \"complexity\": \"${tc.complexity.name}\",\n")
                append("        \"task_type\": \"${tc.taskType.name}\",\n")
                append("        \"input_data\": \"\"\"${tc.inputData}\"\"\",\n")
                append("        \"zero_shot_prompt\": \"\"\"${tc.zeroShotPrompt}\"\"\",\n")
                append("        \"few_shot_prompt\": \"\"\"${tc.fewShotPrompt}\"\"\",\n")
                append("        \"structured_prompt\": \"\"\"${tc.structuredPrompt}\"\"\",\n")
                append("        \"json_schema\": \"\"\"${tc.jsonSchema}\"\"\",\n")
                append("        \"ground_truth\": ${formatMapToPythonDict(tc.groundTruthMap)}\n")
                append("    },\n")
            }
            append("]\n")
        }

        return """#!/usr/bin/env python3
\"\"\"
Prompt Strategy Evaluator: Zero-Shot vs Few-Shot vs Structured
Measures response quality, accuracy, latency, and schema compliance across prompt complexity levels.
Target Model: ${config.model}
\"\"\"

import os
import json
import time
import asyncio
from typing import Dict, Any, List, Optional
from dataclasses import dataclass, asdict
import pandas as pd
import requests

${if (config.includePlotting) "import matplotlib.pyplot as plt\nimport seaborn as sns" else ""}

# 1. API Configuration
GEMINI_API_KEY = os.environ.get("GEMINI_API_KEY", "YOUR_GEMINI_API_KEY")
MODEL_NAME = "${config.model}"
BASE_URL = f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL_NAME}:generateContent?key={GEMINI_API_KEY}"
TRIALS_PER_PROMPT = ${config.trialsPerPrompt}
TEMPERATURE = ${config.temperature}

# 2. Benchmark Evaluation Test Suite
$testCasesJsonLiteral

@dataclass
class MetricResult:
    test_id: str
    complexity: str
    strategy: str
    latency_ms: float
    accuracy_score: float
    schema_valid: bool
    input_tokens_est: int
    output_tokens_est: int
    response_preview: str

def query_gemini_api(prompt: str, json_schema_str: Optional[str] = None) -> Dict[str, Any]:
    headers = {"Content-Type": "application/json"}
    payload = {
        "contents": [{"parts": [{"text": prompt}]}],
        "generationConfig": {
            "temperature": TEMPERATURE,
            "topP": 0.95
        }
    }
    if json_schema_str:
        payload["generationConfig"]["responseMimeType"] = "application/json"

    start_time = time.perf_counter()
    try:
        resp = requests.post(BASE_URL, headers=headers, json=payload, timeout=60)
        latency_ms = (time.perf_counter() - start_time) * 1000.0
        if resp.status_code == 200:
            data = resp.json()
            candidates = data.get("candidates", [])
            text = candidates[0]["content"]["parts"][0]["text"] if candidates else ""
            usage = data.get("usageMetadata", {})
            in_tokens = usage.get("promptTokenCount", len(prompt) // 4)
            out_tokens = usage.get("candidatesTokenCount", len(text) // 4)
            return {
                "text": text,
                "latency_ms": latency_ms,
                "input_tokens": in_tokens,
                "output_tokens": out_tokens,
                "success": True
            }
        else:
            return {
                "text": f"Error {resp.status_code}: {resp.text}",
                "latency_ms": latency_ms,
                "input_tokens": len(prompt) // 4,
                "output_tokens": 0,
                "success": False
            }
    except Exception as e:
        latency_ms = (time.perf_counter() - start_time) * 1000.0
        return {
            "text": f"Exception: {str(e)}",
            "latency_ms": latency_ms,
            "input_tokens": len(prompt) // 4,
            "output_tokens": 0,
            "success": False
        }

def evaluate_accuracy(response_text: str, ground_truth: Dict[str, str], strategy: str) -> (float, bool):
    is_schema_valid = False
    cleaned_text = response_text.strip()
    
    # Strip markdown code fences if model returned ```json ... ```
    if cleaned_text.startswith("```"):
        lines = cleaned_text.splitlines()
        if len(lines) >= 2:
            cleaned_text = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
    
    parsed_json = None
    try:
        parsed_json = json.loads(cleaned_text)
        is_schema_valid = True
    except Exception:
        is_schema_valid = False

    if not ground_truth:
        return (100.0 if is_schema_valid else 70.0), is_schema_valid

    matches = 0
    total_keys = len(ground_truth)
    
    for key, expected_val in ground_truth.items():
        expected_str = str(expected_val).lower().strip()
        if parsed_json and isinstance(parsed_json, dict):
            actual_val = str(parsed_json.get(key, "")).lower().strip()
            if expected_str in actual_val or actual_val in expected_str:
                matches += 1
        else:
            # Fallback substring check in raw text
            if expected_str in cleaned_text.lower():
                matches += 1

    accuracy_score = (matches / total_keys) * 100.0 if total_keys > 0 else 0.0
    if is_schema_valid:
        accuracy_score = min(100.0, accuracy_score + 5.0)
        
    return accuracy_score, is_schema_valid

def run_benchmarks() -> pd.DataFrame:
    print(f"🚀 Starting Prompt Strategy Evaluation Benchmark...")
    print(f"📌 Model: {MODEL_NAME} | Trials per prompt: {TRIALS_PER_PROMPT} | Total Test Cases: {len(TEST_CASES)}")
    print("=" * 80)
    
    results = []
    strategies = ["ZERO_SHOT", "FEW_SHOT", "STRUCTURED"]
    
    for tc in TEST_CASES:
        print(f"\n🧪 Evaluating [{tc['complexity']}] {tc['title']}...")
        
        for strategy in strategies:
            prompt = tc["zero_shot_prompt"] if strategy == "ZERO_SHOT" else (
                tc["few_shot_prompt"] if strategy == "FEW_SHOT" else tc["structured_prompt"]
            )
            schema = tc["json_schema"] if strategy == "STRUCTURED" else None
            
            latencies = []
            accuracies = []
            valid_schemas = 0
            in_tokens_list = []
            out_tokens_list = []
            last_resp = ""
            
            for trial in range(TRIALS_PER_PROMPT):
                res = query_gemini_api(prompt, schema)
                latencies.append(res["latency_ms"])
                in_tokens_list.append(res["input_tokens"])
                out_tokens_list.append(res["output_tokens"])
                last_resp = res["text"]
                
                acc, is_valid = evaluate_accuracy(res["text"], tc["ground_truth"], strategy)
                accuracies.append(acc)
                if is_valid:
                    valid_schemas += 1
                    
            avg_latency = sum(latencies) / len(latencies)
            avg_accuracy = sum(accuracies) / len(accuracies)
            schema_pct = (valid_schemas / TRIALS_PER_PROMPT) * 100.0
            
            results.append(MetricResult(
                test_id=tc["id"],
                complexity=tc["complexity"],
                strategy=strategy,
                latency_ms=round(avg_latency, 2),
                accuracy_score=round(avg_accuracy, 2),
                schema_valid=(valid_schemas > 0),
                input_tokens_est=int(sum(in_tokens_list) / len(in_tokens_list)),
                output_tokens_est=int(sum(out_tokens_list) / len(out_tokens_list)),
                response_preview=last_resp[:90].replace("\n", " ") + "..."
            ))
            
            print(f"  • {strategy:<12} -> Accuracy: {avg_accuracy:5.1f}% | Latency: {avg_latency:6.1f}ms | Schema Valid: {schema_pct:3.0f}%")
            
    df = pd.DataFrame([asdict(r) for r in results])
    return df

def summarize_and_visualize(df: pd.DataFrame):
    print("\n" + "=" * 80)
    print("📊 COMPARATIVE METRICS SUMMARY TABLE")
    print("=" * 80)
    
    summary = df.groupby(["complexity", "strategy"]).agg(
        Mean_Accuracy=("accuracy_score", "mean"),
        Mean_Latency_ms=("latency_ms", "mean"),
        Avg_Input_Tokens=("input_tokens_est", "mean"),
        Avg_Output_Tokens=("output_tokens_est", "mean")
    ).round(2)
    
    print(summary.to_string())
    
    overall = df.groupby("strategy").agg(
        Overall_Accuracy=("accuracy_score", "mean"),
        Overall_Latency_ms=("latency_ms", "mean"),
        Total_Input_Tokens=("input_tokens_est", "sum")
    ).round(2)
    
    print("\n🏆 OVERALL STRATEGY RANKING:")
    print(overall.to_string())

${if (config.includePlotting) """
    # Plotting Comparative Metrics Chart
    try:
        fig, axes = plt.subplots(1, 2, figsize=(14, 5))
        sns.barplot(data=df, x="complexity", y="accuracy_score", hue="strategy", ax=axes[0], palette="viridis")
        axes[0].set_title("Accuracy (%) across Complexity Levels")
        axes[0].set_ylim(0, 105)
        
        sns.barplot(data=df, x="complexity", y="latency_ms", hue="strategy", ax=axes[1], palette="mako")
        axes[1].set_title("Latency (ms) across Complexity Levels")
        
        plt.tight_layout()
        plt.savefig("prompt_strategy_comparison.png", dpi=300)
        print("\n📈 Chart exported successfully as 'prompt_strategy_comparison.png'")
    except Exception as e:
        print(f"Note: Matplotlib export skipped: {e}")
""" else ""}

if __name__ == "__main__":
    df_results = run_benchmarks()
    summarize_and_visualize(df_results)
"""
    }

    private fun escapePythonString(str: String): String {
        return str.replace("\"", "\\\"").replace("\n", " ")
    }

    private fun formatMapToPythonDict(map: Map<String, String>): String {
        if (map.isEmpty()) return "{}"
        val entries = map.entries.joinToString(", ") { "\"${it.key}\": \"${it.value}\"" }
        return "{$entries}"
    }
}
