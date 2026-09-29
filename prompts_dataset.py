"""
Prompt Benchmark Test Suite & Dataset
Contains standardized evaluation cases across Low, Medium, High, and Expert complexity levels.
"""

TEST_CASES = [
    # ================= LOW COMPLEXITY =================
    {
        "id": "low_sentiment_urgency",
        "title": "Customer Feedback Urgency & Sentiment",
        "complexity": "LOW",
        "task_type": "SENTIMENT_ANALYSIS",
        "system_instruction": "You are a customer feedback evaluation assistant. Extract the exact sentiment and urgency rating.",
        "input_data": "Text: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'",
        "zero_shot_prompt": "Analyze the text and output sentiment (POSITIVE, NEUTRAL, or NEGATIVE) and urgency rating (1-5):\n\nText: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'",
        "few_shot_prompt": """Analyze the text and output JSON with sentiment, urgency (1-5), and reason.

Example 1:
Input: Text: 'App crashed once while opening, but reopened fine on second try.'
Output: {"sentiment": "NEUTRAL", "urgency": 2, "reason": "Minor transient crash"}

Example 2:
Input: Text: 'Loving the new Dark Mode update, great work team!'
Output: {"sentiment": "POSITIVE", "urgency": 1, "reason": "Praise feedback"}

Example 3:
Input: Text: 'Cannot access my server data! Production is down immediately!'
Output: {"sentiment": "NEGATIVE", "urgency": 5, "reason": "Critical outage"}

Now classify this:
Input: Text: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'
Output:""",
        "structured_prompt": """Analyze the customer feedback text according to this strict JSON schema. Perform brief chain-of-thought analysis in reasoning before final classification.

Input: Text: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "reasoning": { "type": "string" },
    "sentiment": { "type": "string", "enum": ["POSITIVE", "NEUTRAL", "NEGATIVE"] },
    "urgency": { "type": "integer", "minimum": 1, "maximum": 5 },
    "category": { "type": "string", "enum": ["BILLING", "TECHNICAL", "FEATURE", "GENERAL"] }
  },
  "required": ["reasoning", "sentiment", "urgency", "category"]
}""",
        "ground_truth": {
            "sentiment": "NEGATIVE",
            "urgency": "5",
            "category": "BILLING"
        }
    },
    {
        "id": "low_ner_extraction",
        "title": "Named Entity & Date Disambiguation",
        "complexity": "LOW",
        "task_type": "ENTITY_EXTRACTION",
        "system_instruction": "You are an entity extraction engine. Extract person, organization, date, and city precisely.",
        "input_data": "Announcement: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'",
        "zero_shot_prompt": "Extract the Person, Organization, Date (YYYY-MM-DD), and Location from this text:\n\n'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'",
        "few_shot_prompt": """Extract entities formatted as JSON.

Example 1:
Input: 'Marcus Vance of Apex Dynamics spoke on July 4, 2025 in Berlin.'
Output: {"person": "Marcus Vance", "organization": "Apex Dynamics", "date": "2025-07-04", "location": "Berlin"}

Example 2:
Input: 'Sarah Lin presented for BioTech Global in Tokyo on Jan 12, 2026.'
Output: {"person": "Sarah Lin", "organization": "BioTech Global", "date": "2026-01-12", "location": "Tokyo"}

Now extract:
Input: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'
Output:""",
        "structured_prompt": """Extract named entities adhering strictly to the JSON schema. Standardize date format to ISO YYYY-MM-DD.

Input: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "person": { "type": "string" },
    "organization": { "type": "string" },
    "date": { "type": "string" },
    "location": { "type": "string" }
  },
  "required": ["person", "organization", "date", "location"]
}""",
        "ground_truth": {
            "person": "Elena Rostova",
            "organization": "NeoGen Labs",
            "date": "2026-10-14",
            "location": "Zurich"
        }
    },

    # ================= MEDIUM COMPLEXITY =================
    {
        "id": "med_text_to_sql",
        "title": "Text-to-SQL with Table Schema Join",
        "complexity": "MEDIUM",
        "task_type": "CODE_SQL_GENERATION",
        "system_instruction": "You are a senior PostgreSQL engineer. Convert natural language questions into efficient, safe SQL queries.",
        "input_data": "Tables: `orders (order_id, user_id, order_total, created_at)` and `users (user_id, country, tier)`. Query: 'Find the top 5 users in Japan by total spend in 2025 who are in the GOLD tier.'",
        "zero_shot_prompt": """Write a PostgreSQL query for this request:
Tables: `orders (order_id, user_id, order_total, created_at)` and `users (user_id, country, tier)`.
Request: 'Find the top 5 users in Japan by total spend in 2025 who are in the GOLD tier.'""",
        "few_shot_prompt": """Convert schema questions to SQL queries.

Example 1:
Schema: `products(id, category, price)`
Request: 'Top 3 cheapest books'
Output: {"sql": "SELECT id, price FROM products WHERE category = 'Books' ORDER BY price ASC LIMIT 3;", "tables_used": ["products"]}

Example 2:
Schema: `customers(id, city)`, `payments(id, customer_id, amount, date)`
Request: 'Total payment volume from Paris in 2024'
Output: {"sql": "SELECT SUM(p.amount) AS total_volume FROM payments p JOIN customers c ON p.customer_id = c.id WHERE c.city = 'Paris' AND p.date >= '2024-01-01' AND p.date <= '2024-12-31';", "tables_used": ["customers", "payments"]}

Now write query for:
Schema: `orders (order_id, user_id, order_total, created_at)` and `users (user_id, country, tier)`
Request: 'Find the top 5 users in Japan by total spend in 2025 who are in the GOLD tier.'
Output:""",
        "structured_prompt": """Generate PostgreSQL query and execution metadata according to schema definition. Validate date filter logic.

Tables: `orders (order_id, user_id, order_total, created_at)` and `users (user_id, country, tier)`.
Question: 'Find the top 5 users in Japan by total spend in 2025 who are in the GOLD tier.'""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "sql_query": { "type": "string" },
    "primary_tables": { "type": "array", "items": { "type": "string" } },
    "aggregate_functions": { "type": "array", "items": { "type": "string" } },
    "limit_count": { "type": "integer" }
  },
  "required": ["sql_query", "primary_tables", "limit_count"]
}""",
        "ground_truth": {
            "tables": "orders, users",
            "limit": "5",
            "aggregation": "SUM"
        }
    },
    {
        "id": "med_contract_clause",
        "title": "Legal SLA & Liability Clause Triage",
        "complexity": "MEDIUM",
        "task_type": "LEGAL_CONTRACT_ANALYSIS",
        "system_instruction": "You are a legal risk analyst. Identify liability caps, cure periods, and indemnification risk levels.",
        "input_data": "Clause 8.2: 'In no event shall either party's aggregate liability under this Master Agreement exceed twelve (12) months of fees paid prior to the claim. Either party may terminate with 30 days written notice upon uncured breach.'",
        "zero_shot_prompt": "Analyze this contract clause and extract Liability Cap, Cure/Notice Period, and Risk Level (LOW, MEDIUM, HIGH):\n\nClause: 'In no event shall either party's aggregate liability under this Master Agreement exceed twelve (12) months of fees paid prior to the claim. Either party may terminate with 30 days written notice upon uncured breach.'",
        "few_shot_prompt": """Analyze contract terms into structured JSON.

Example 1:
Clause: 'Liability is uncapped for gross negligence. 10 days notice required.'
Output: {"liability_cap": "UNCAPPED", "notice_period_days": 10, "risk_level": "HIGH"}

Example 2:
Clause: 'Liability capped at \$50,000. Cure period is 60 days.'
Output: {"liability_cap": "50000 USD", "notice_period_days": 60, "risk_level": "LOW"}

Now analyze:
Clause: 'In no event shall either party's aggregate liability under this Master Agreement exceed twelve (12) months of fees paid prior to the claim. Either party may terminate with 30 days written notice upon uncured breach.'
Output:""",
        "structured_prompt": """Extract contract risk terms and parameters adhering to the strict schema.

Clause: 'In no event shall either party's aggregate liability under this Master Agreement exceed twelve (12) months of fees paid prior to the claim. Either party may terminate with 30 days written notice upon uncured breach.'""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "liability_cap_type": { "type": "string", "enum": ["12_MONTHS_FEES", "FIXED_AMOUNT", "UNCAPPED"] },
    "termination_notice_days": { "type": "integer" },
    "risk_classification": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH"] }
  },
  "required": ["liability_cap_type", "termination_notice_days", "risk_classification"]
}""",
        "ground_truth": {
            "liability_cap_type": "12_MONTHS_FEES",
            "termination_notice_days": "30",
            "risk_classification": "MEDIUM"
        }
    },

    # ================= HIGH COMPLEXITY =================
    {
        "id": "high_medical_triage",
        "title": "Clinical Phenotype & ICD-10 Code Extraction",
        "complexity": "HIGH",
        "task_type": "MEDICAL_TRIAGE_REASONING",
        "system_instruction": "You are a clinical NLP reasoning engine. Extract primary symptoms, ICD-10 suggestions, and contraindication flags with rigorous medical caution.",
        "input_data": "Patient 64M with acute shortness of breath (dyspnea), bilateral lower extremity pitting edema, BNP 980 pg/mL, history of chronic hypertension and type 2 diabetes with mild renal impairment (eGFR 48). Current Rx: Metformin 1000mg, Lisinopril 20mg.",
        "zero_shot_prompt": """Review clinical note and list primary diagnosis suspicion (with ICD-10 code), symptom triad, and medication caution for renal function:

'Patient 64M with acute shortness of breath (dyspnea), bilateral lower extremity pitting edema, BNP 980 pg/mL, history of chronic hypertension and type 2 diabetes with mild renal impairment (eGFR 48). Current Rx: Metformin 1000mg, Lisinopril 20mg.'""",
        "few_shot_prompt": """Analyze clinical findings into structured medical JSON.

Example 1:
Input: '72F sudden onset chest pain radiating to left arm, Troponin I elevated at 2.4 ng/mL, ST-elevations in V2-V4.'
Output: {"suspected_condition": "Acute Anterior STEMI", "icd10_code": "I21.0", "urgency": "EMERGENCY", "med_warning": "Immediate cath lab activation"}

Example 2:
Input: '45M wheezing, cough, history of asthma, peak flow 60% of baseline, currently on Albuterol PRN.'
Output: {"suspected_condition": "Asthma Exacerbation", "icd10_code": "J45.901", "urgency": "URGENT", "med_warning": "Systemic corticosteroid evaluation"}

Now analyze:
Input: 'Patient 64M with acute shortness of breath (dyspnea), bilateral lower extremity pitting edema, BNP 980 pg/mL, history of chronic hypertension and type 2 diabetes with mild renal impairment (eGFR 48). Current Rx: Metformin 1000mg, Lisinopril 20mg.'
Output:""",
        "structured_prompt": """Process clinical note into verified structured JSON. Perform differential reasoning before code selection.

Clinical Note: 'Patient 64M with acute shortness of breath (dyspnea), bilateral lower extremity pitting edema, BNP 980 pg/mL, history of chronic hypertension and type 2 diabetes with mild renal impairment (eGFR 48). Current Rx: Metformin 1000mg, Lisinopril 20mg.'""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "primary_diagnosis": { "type": "string" },
    "icd10_code": { "type": "string" },
    "metformin_caution_flag": { "type": "boolean" },
    "cardiac_biomarker_elevated": { "type": "boolean" },
    "triage_priority": { "type": "string", "enum": ["STAT", "URGENT", "ROUTINE"] }
  },
  "required": ["primary_diagnosis", "icd10_code", "metformin_caution_flag", "cardiac_biomarker_elevated", "triage_priority"]
}""",
        "ground_truth": {
            "icd10": "I50",
            "diagnosis": "Heart Failure",
            "metformin_flag": "true",
            "biomarker": "true"
        }
    },

    # ================= EXPERT COMPLEXITY =================
    {
        "id": "expert_multistep_logic",
        "title": "Supply Chain Route Optimization & Constraint Solving",
        "complexity": "EXPERT",
        "task_type": "MULTI_STEP_LOGIC_SOLVER",
        "system_instruction": "You are a logistics optimization engine. Solve multi-constraint routing, compute minimal cost, and ensure no capacity or time window violations.",
        "input_data": "Warehouse W has 500 units. Depot A needs 180 units (deadline 11:00, penalty \$50/hr late), Depot B needs 220 units (deadline 13:00), Depot C needs 150 units (deadline 15:00). Truck 1 (cap 300, speed 50km/h, cost \$2/km) and Truck 2 (cap 250, speed 60km/h, cost \$2.5/km). Distances: W->A: 40km, W->B: 70km, A->B: 35km, B->C: 45km, W->C: 90km. Start at 08:00.",
        "zero_shot_prompt": """Solve the logistics problem:
Warehouse W has 500 units.
Depot A needs 180 units (deadline 11:00).
Depot B needs 220 units (deadline 13:00).
Depot C needs 150 units (deadline 15:00).
Truck 1: cap 300, speed 50km/h, cost \$2/km.
Truck 2: cap 250, speed 60km/h, cost \$2.5/km.
Distances: W->A: 40km, W->B: 70km, A->B: 35km, B->C: 45km, W->C: 90km.
Find feasible truck dispatch route, total distance, and lowest transport cost with zero deadline violations.""",
        "few_shot_prompt": """Solve vehicle routing constraints into structured JSON.

Example 1:
Problem: W(100) -> D1(60, 20km), D2(40, 30km). Truck 1 (cap 100, \$1/km).
Output: {"routes": [{"truck": 1, "path": ["W", "D1", "D2"], "load": 100, "distance_km": 50}], "total_cost": 50, "all_deadlines_met": true}

Now solve:
Warehouse W has 500 units.
Depot A needs 180 units (deadline 11:00).
Depot B needs 220 units (deadline 13:00).
Depot C needs 150 units (deadline 15:00).
Truck 1: cap 300, speed 50km/h, cost \$2/km.
Truck 2: cap 250, speed 60km/h, cost \$2.5/km.
Distances: W->A: 40km, W->B: 70km, A->B: 35km, B->C: 45km, W->C: 90km. Start 08:00.
Output:""",
        "structured_prompt": """Perform step-by-step route planning calculation and output strictly to JSON schema. Verify truck capacity limits (T1<=300, T2<=250) and arrival time windows.

Problem Data:
Warehouse W: 500 units.
Depot A: 180 units (deadline 11:00).
Depot B: 220 units (deadline 13:00).
Depot C: 150 units (deadline 15:00).
Truck 1: cap 300, speed 50km/h, cost \$2/km.
Truck 2: cap 250, speed 60km/h, cost \$2.5/km.
Distances: W->A: 40km, W->B: 70km, A->B: 35km, B->C: 45km, W->C: 90km. Start 08:00.""",
        "json_schema": """{
  "type": "object",
  "properties": {
    "step_by_step_reasoning": { "type": "string" },
    "truck1_route": { "type": "array", "items": { "type": "string" } },
    "truck1_load": { "type": "integer" },
    "truck2_route": { "type": "array", "items": { "type": "string" } },
    "truck2_load": { "type": "integer" },
    "total_cost_usd": { "type": "number" },
    "all_deadlines_met": { "type": "boolean" }
  },
  "required": ["step_by_step_reasoning", "truck1_route", "truck1_load", "truck2_route", "truck2_load", "total_cost_usd", "all_deadlines_met"]
}""",
        "ground_truth": {
            "deadlines_met": "true",
            "capacity_valid": "true"
        }
    }
]
