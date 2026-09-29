#!/usr/bin/env python3
"""
Prompt Strategy Benchmark: Zero-Shot vs Few-Shot vs Structured Prompts
Evaluates Accuracy, Latency, Token Usage, and Schema Compliance across prompt complexity levels.

Usage:
    python benchmark.py
    python benchmark.py --model gemini-2.5-flash --trials 3 --plot
    python benchmark.py --complexity HIGH --output results.csv
"""

import os
import sys
import json
import time
import argparse
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, asdict
import requests

# Import benchmark test cases dataset
try:
    from prompts_dataset import TEST_CASES
except ImportError:
    # If run from different directory
    import sys
    sys.path.append(os.path.dirname(os.path.abspath(__file__)))
    from prompts_dataset import TEST_CASES

@dataclass
class TrialMetric:
    test_id: str
    title: str
    complexity: str
    strategy: str  # ZERO_SHOT, FEW_SHOT, STRUCTURED
    trial_index: int
    latency_ms: float
    accuracy_score: float
    schema_valid: bool
    input_tokens: int
    output_tokens: int
    response_preview: str
    success: bool

class PromptBenchmarkRunner:
    def __init__(
        self,
        api_key: Optional[str] = None,
        model_name: str = "gemini-2.5-flash",
        temperature: float = 0.2,
        trials_per_strategy: int = 2,
        simulate_if_no_key: bool = True
    ):
        self.api_key = api_key or os.environ.get("GEMINI_API_KEY", "")
        self.model_name = model_name
        self.temperature = temperature
        self.trials = trials_per_strategy
        self.simulate_mode = not bool(self.api_key) and simulate_if_no_key
        
        if not self.api_key and not self.simulate_mode:
            print("⚠️ Warning: No GEMINI_API_KEY provided. Running in simulation mode.")
            self.simulate_mode = True

    def query_model(self, prompt: str, system_instruction: Optional[str] = None, json_schema_str: Optional[str] = None) -> Dict[str, Any]:
        """Queries the Gemini model via REST API or simulates deterministic responses."""
        if self.simulate_mode:
            return self._simulate_response(prompt, json_schema_str)

        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.model_name}:generateContent?key={self.api_key}"
        headers = {"Content-Type": "application/json"}
        
        payload: Dict[str, Any] = {
            "contents": [{"parts": [{"text": prompt}]}],
            "generationConfig": {
                "temperature": self.temperature,
                "topP": 0.95
            }
        }
        
        if system_instruction:
            payload["systemInstruction"] = {
                "parts": [{"text": system_instruction}]
            }

        if json_schema_str:
            try:
                schema_obj = json.loads(json_schema_str)
                payload["generationConfig"]["responseMimeType"] = "application/json"
                payload["generationConfig"]["responseSchema"] = schema_obj
            except Exception:
                payload["generationConfig"]["responseMimeType"] = "application/json"

        start_time = time.perf_counter()
        try:
            resp = requests.post(url, headers=headers, json=payload, timeout=60)
            latency_ms = (time.perf_counter() - start_time) * 1000.0
            
            if resp.status_code == 200:
                data = resp.json()
                candidates = data.get("candidates", [])
                text = ""
                if candidates and "content" in candidates[0]:
                    parts = candidates[0]["content"].get("parts", [])
                    if parts:
                        text = parts[0].get("text", "")
                
                usage = data.get("usageMetadata", {})
                in_tok = usage.get("promptTokenCount", len(prompt) // 4)
                out_tok = usage.get("candidatesTokenCount", len(text) // 4)
                
                return {
                    "text": text,
                    "latency_ms": latency_ms,
                    "input_tokens": in_tok,
                    "output_tokens": out_tok,
                    "success": True
                }
            else:
                return {
                    "text": f"Error HTTP {resp.status_code}: {resp.text[:200]}",
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

    def _simulate_response(self, prompt: str, schema_str: Optional[str]) -> Dict[str, Any]:
        """Provides realistic simulation when running offline or without API key."""
        time.sleep(0.08)  # simulate brief network round-trip
        is_structured = schema_str is not None
        is_few_shot = "Example 1:" in prompt
        
        latency = 320.0 if not is_structured else 420.0
        if is_few_shot:
            latency += 90.0

        if is_structured:
            output = '{"sentiment": "NEGATIVE", "urgency": 5, "category": "BILLING", "status": "COMPLETED"}'
        elif is_few_shot:
            output = '{"sentiment": "NEGATIVE", "urgency": 5, "reason": "Customer billing charge duplication"}'
        else:
            output = "The customer review expresses a NEGATIVE sentiment with an urgency rating of 5 due to double charging."

        return {
            "text": output,
            "latency_ms": latency,
            "input_tokens": len(prompt) // 4,
            "output_tokens": len(output) // 4,
            "success": True
        }

    def evaluate_output(self, response_text: str, ground_truth: Dict[str, str], strategy: str) -> Tuple[float, bool]:
        """Scores response based on key ground truth matches and schema validity."""
        cleaned = response_text.strip()
        if cleaned.startswith("```"):
            lines = cleaned.splitlines()
            if len(lines) >= 2:
                cleaned = "\n".join(lines[1:-1] if lines[-1].strip() == "```" else lines[1:])
        
        is_schema_valid = False
        parsed_json = None
        try:
            parsed_json = json.loads(cleaned)
            is_schema_valid = True
        except Exception:
            is_schema_valid = False

        score = 0.0
        total_checks = max(1, len(ground_truth))
        matched = 0

        target_text = response_text.lower()
        for key, val in ground_truth.items():
            expected_val = str(val).lower()
            if parsed_json and isinstance(parsed_json, dict):
                found_val = str(parsed_json.get(key, "")).lower()
                if expected_val in found_val or found_val in expected_val:
                    matched += 1
                    continue
            if expected_val in target_text:
                matched += 1

        base_score = (matched / total_checks) * 80.0
        
        if strategy == "STRUCTURED":
            schema_bonus = 20.0 if is_schema_valid else 0.0
            score = min(100.0, base_score + schema_bonus)
        elif strategy == "FEW_SHOT":
            schema_bonus = 15.0 if is_schema_valid else 5.0
            score = min(100.0, base_score + schema_bonus)
        else: # ZERO_SHOT
            score = min(100.0, base_score + 10.0)

        return round(score, 1), is_schema_valid

    def run_suite(self, complexity_filter: Optional[str] = None) -> List[TrialMetric]:
        """Runs benchmark across all test cases and prompt strategies."""
        results: List[TrialMetric] = []
        cases = TEST_CASES
        if complexity_filter:
            cases = [c for c in cases if c["complexity"].upper() == complexity_filter.upper()]

        strategies = [
            ("ZERO_SHOT", lambda tc: (tc["zero_shot_prompt"], None)),
            ("FEW_SHOT", lambda tc: (tc["few_shot_prompt"], None)),
            ("STRUCTURED", lambda tc: (tc["structured_prompt"], tc.get("json_schema")))
        ]

        total_runs = len(cases) * len(strategies) * self.trials
        print(f"\n==================================================")
        print(f"🚀 Starting Prompt Benchmark Suite")
        print(f"   Model: {self.model_name} | Trials: {self.trials} per strategy")
        print(f"   Test Cases: {len(cases)} | Total Executions: {total_runs}")
        print(f"   Mode: {'⚡ Live Gemini API' if not self.simulate_mode else '💻 Offline Simulation'}")
        print(f"==================================================\n")

        current = 0
        for tc in cases:
            print(f"▶ [{tc['complexity']}] {tc['title']}")
            for strategy_name, prompt_fn in strategies:
                prompt, schema = prompt_fn(tc)
                sys_inst = tc.get("system_instruction")

                for t in range(self.trials):
                    current += 1
                    resp = self.query_model(prompt, sys_inst, schema)
                    score, schema_valid = self.evaluate_output(
                        resp["text"], tc.get("ground_truth", {}), strategy_name
                    )

                    metric = TrialMetric(
                        test_id=tc["id"],
                        title=tc["title"],
                        complexity=tc["complexity"],
                        strategy=strategy_name,
                        trial_index=t + 1,
                        latency_ms=round(resp["latency_ms"], 1),
                        accuracy_score=score,
                        schema_valid=schema_valid,
                        input_tokens=resp["input_tokens"],
                        output_tokens=resp["output_tokens"],
                        response_preview=resp["text"].replace("\n", " ")[:60],
                        success=resp["success"]
                    )
                    results.append(metric)

                # Summary line for this strategy
                strat_metrics = [m for m in results if m.test_id == tc["id"] and m.strategy == strategy_name]
                avg_acc = sum(m.accuracy_score for m in strat_metrics) / len(strat_metrics)
                avg_lat = sum(m.latency_ms for m in strat_metrics) / len(strat_metrics)
                valid_rate = sum(1 for m in strat_metrics if m.schema_valid) / len(strat_metrics) * 100
                print(f"   • {strategy_name:<12} -> Avg Accuracy: {avg_acc:5.1f}% | Latency: {avg_lat:5.0f}ms | JSON Valid: {valid_rate:3.0f}%")
            print()

        return results

def print_summary_table(results: List[TrialMetric]):
    """Prints a consolidated Markdown / Terminal summary table."""
    try:
        import pandas as pd
        from tabulate import tabulate
        df = pd.DataFrame([asdict(r) for r in results])
        
        summary = df.groupby(["strategy", "complexity"]).agg({
            "accuracy_score": "mean",
            "latency_ms": "mean",
            "schema_valid": lambda x: (x.sum() / len(x)) * 100,
            "input_tokens": "mean",
            "output_tokens": "mean"
        }).reset_index()

        summary.columns = ["Strategy", "Complexity", "Accuracy (%)", "Avg Latency (ms)", "Schema Valid (%)", "In Tokens", "Out Tokens"]
        summary["Accuracy (%)"] = summary["Accuracy (%)"].round(1)
        summary["Avg Latency (ms)"] = summary["Avg Latency (ms)"].round(0)
        summary["Schema Valid (%)"] = summary["Schema Valid (%)"].round(1)
        summary["In Tokens"] = summary["In Tokens"].round(0)
        summary["Out Tokens"] = summary["Out Tokens"].round(0)

        print("\n================== BENCHMARK SUMMARY ==================")
        print(tabulate(summary, headers="keys", tablefmt="github", showindex=False))
        print("=======================================================\n")
    except Exception:
        # Basic fallback without pandas/tabulate
        print("\n--- Summary Results ---")
        for s in ["ZERO_SHOT", "FEW_SHOT", "STRUCTURED"]:
            s_metrics = [r for r in results if r.strategy == s]
            if s_metrics:
                avg_acc = sum(r.accuracy_score for r in s_metrics) / len(s_metrics)
                avg_lat = sum(r.latency_ms for r in s_metrics) / len(s_metrics)
                print(f"Strategy: {s:<12} | Avg Accuracy: {avg_acc:.1f}% | Avg Latency: {avg_lat:.0f}ms")

def save_plot(results: List[TrialMetric], filename: str = "benchmark_results.png"):
    """Generates comparison charts for Accuracy and Latency across strategies."""
    try:
        import matplotlib.pyplot as plt
        import pandas as pd
        import seaborn as sns

        df = pd.DataFrame([asdict(r) for r in results])
        fig, axes = plt.subplots(1, 2, figsize=(14, 5))
        
        sns.set_theme(style="whitegrid")

        # Plot 1: Accuracy by Strategy & Complexity
        sns.barplot(
            data=df,
            x="complexity",
            y="accuracy_score",
            hue="strategy",
            ax=axes[0],
            palette="Blues_d",
            order=["LOW", "MEDIUM", "HIGH", "EXPERT"]
        )
        axes[0].set_title("Accuracy Score by Prompt Strategy & Complexity", fontsize=12, fontweight="bold")
        axes[0].set_ylabel("Accuracy Score (0 - 100)")
        axes[0].set_xlabel("Complexity Level")
        axes[0].set_ylim(0, 105)

        # Plot 2: Latency by Strategy
        sns.barplot(
            data=df,
            x="complexity",
            y="latency_ms",
            hue="strategy",
            ax=axes[1],
            palette="Oranges_d",
            order=["LOW", "MEDIUM", "HIGH", "EXPERT"]
        )
        axes[1].set_title("Response Latency (ms) by Strategy", fontsize=12, fontweight="bold")
        axes[1].set_ylabel("Latency (ms)")
        axes[1].set_xlabel("Complexity Level")

        plt.tight_layout()
        plt.savefig(filename, dpi=300)
        print(f"📊 Chart successfully saved to: {filename}")
    except Exception as e:
        print(f"⚠️ Could not generate chart: {e}")

def main():
    parser = argparse.ArgumentParser(description="Prompt Strategy Benchmark Runner")
    parser.add_argument("--api-key", type=str, help="Google Gemini API Key (or set GEMINI_API_KEY env)")
    parser.add_argument("--model", type=str, default="gemini-2.5-flash", help="Model name (e.g. gemini-2.5-flash, gemini-1.5-pro)")
    parser.add_argument("--trials", type=int, default=2, help="Number of trials per test case & strategy")
    parser.add_argument("--complexity", type=str, choices=["LOW", "MEDIUM", "HIGH", "EXPERT"], help="Filter by complexity level")
    parser.add_argument("--output", type=str, default="benchmark_results.csv", help="CSV output filename")
    parser.add_argument("--plot", action="store_true", help="Generate and save graphical comparison plots")
    args = parser.parse_args()

    runner = PromptBenchmarkRunner(
        api_key=args.api_key,
        model_name=args.model,
        trials_per_strategy=args.trials
    )

    results = runner.run_suite(complexity_filter=args.complexity)
    print_summary_table(results)

    # Save to CSV
    try:
        import pandas as pd
        df = pd.DataFrame([asdict(r) for r in results])
        df.to_csv(args.output, index=False)
        print(f"💾 Detailed results exported to: {args.output}")
    except Exception as e:
        print(f"⚠️ Could not export to CSV: {e}")

    if args.plot:
        save_plot(results)

if __name__ == "__main__":
    main()
