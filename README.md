# Prompt Strategy Evaluator & Visualizer

A complete Android & Python benchmarking suite that evaluates how **Zero-Shot**, **Few-Shot**, and **Structured** prompt strategies affect response quality, accuracy, latency, and token consumption across varying prompt complexity levels (Low, Medium, High).

Built with **Jetpack Compose (Material 3 - Clean Minimalism)** on Android and paired with ready-to-run **Python benchmarking files** (`benchmark.py` and `prompts_dataset.py`).

---

## ⚡ Can I clone and run this in Android Studio on my PC?

**Yes, absolutely!** You can clone or download this project and run it in Android Studio within minutes. Follow the quick guide below:

### Quick Run in Android Studio (Step-by-Step)

#### Step 1: Get the Code
Clone or download this project repository:
```bash
git clone <YOUR_REPO_URL>
cd prompt-strategy
```

#### Step 2: Open in Android Studio
1. Launch **Android Studio**.
2. Click **Open** (or `File > Open...`).
3. Select the root folder of this project.
4. Android Studio will automatically recognize the Gradle project and trigger a **Gradle Sync**.

#### Step 3: (Optional) Set Your Gemini API Key
The app has a **built-in offline simulator** so it works immediately even without an API key!
To test live model calls with the Gemini API:
1. Copy `.env.example` to `.env` in the root folder:
   ```bash
   cp .env.example .env
   ```
2. Open `.env` and paste your key:
   ```properties
   GEMINI_API_KEY=your_actual_gemini_api_key_here
   ```
   *(You can obtain your Gemini API key from Google Developer Console)*

#### Step 4: Click Run ▶
- Select an **Android Emulator** (API 26+) or connect your **physical Android device** via USB debugging.
- Click the green **Run** (▶) button or press `Shift + F10`.
- The app compiles and installs onto your device!

---

## 🐍 Running the Python Benchmark Files on Your PC

In addition to the Android app, this repository includes standalone Python files for batch evaluation, latency analysis, and plotting.

### 1. Install Requirements
Open your PC terminal or command prompt inside the project folder:
```bash
pip install -r requirements.txt
```

### 2. (Optional) Provide Gemini API Key
```bash
# On Windows (Command Prompt)
set GEMINI_API_KEY=your_gemini_api_key_here

# On Windows (PowerShell)
$env:GEMINI_API_KEY="your_gemini_api_key_here"

# On macOS / Linux
export GEMINI_API_KEY="your_gemini_api_key_here"
```
*(If no API key is set, `benchmark.py` automatically runs in deterministic simulation mode so you can test immediately without any errors).*

### 3. Run the Benchmark
```bash
python benchmark.py
```

### Advanced CLI Options
- **Run with specific prompt complexity:**
  ```bash
  python benchmark.py --complexity HIGH
  ```
  *(Options: `LOW`, `MEDIUM`, `HIGH`, `ALL`)*

- **Export results to CSV:**
  ```bash
  python benchmark.py --output results.csv
  ```

- **Generate comparison charts:**
  ```bash
  python benchmark.py --plot
  ```

- **Change model or number of trials:**
  ```bash
  python benchmark.py --model gemini-2.5-flash --trials 3
  ```

---

## 📁 Repository Structure

```text
├── README.md               # Quick-start guide & documentation
├── benchmark.py            # Python benchmark engine & CLI tool
├── prompts_dataset.py      # Benchmark test cases across complexity tiers
├── requirements.txt        # Python dependencies
├── metadata.json           # Platform project metadata
├── .env.example            # Environment variable template for API key
│
├── app/                    # Android application module
│   ├── build.gradle.kts    # App-level dependencies & build setup
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/
│       │   ├── MainActivity.kt
│       │   ├── data/       # Models, BenchmarkEngine, Test cases
│       │   └── ui/         # Jetpack Compose UI (Screens, Cards, Charts, Theme)
│       └── res/            # Strings, drawables, theme XMLs
```

---

## 📊 Prompt Strategies Compared

| Strategy | Description | Typical Accuracy | Latency Profile | Best Used For |
|---|---|---|---|---|
| **Zero-Shot** | Direct task instruction without examples. | Baseline (50–70%) | Lowest latency, lowest cost | Quick answers, simple reasoning, creative tasks |
| **Few-Shot** | Includes 2–4 verified input/output examples. | High (80–90%) | Moderate latency (+15–30% tokens) | Format consistency, domain-specific classification |
| **Structured** | Enforces strict JSON Schema / typed output. | Highest (90–98%) | Predictable latency & schema reliability | API integrations, entity extraction, data pipelines |

---

## 🛠️ Troubleshooting & Tips

- **Gradle Sync fails in Android Studio?**
  - Verify your Gradle JDK in `Settings > Build, Execution, Deployment > Build Tools > Gradle` is set to **JDK 17** or **JDK 21**.
  - Click `File > Sync Project with Gradle Files`.
- **Offline / No internet access?**
  - The Android app and Python benchmark automatically detect if no API key is present and gracefully run in **deterministic test mode**, allowing full UI visualization and metrics testing without errors.
