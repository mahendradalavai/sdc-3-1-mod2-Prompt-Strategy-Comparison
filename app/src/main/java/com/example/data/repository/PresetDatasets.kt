package com.example.data.repository

import com.example.data.model.BenchmarkTestCase
import com.example.data.model.BenchmarkTaskType
import com.example.data.model.ComplexityLevel
import com.example.data.model.FewShotExample

object PresetDatasets {

    val allTestCases: List<BenchmarkTestCase> = listOf(
        // ================= LOW COMPLEXITY =================
        BenchmarkTestCase(
            id = "low_sentiment_urgency",
            title = "Customer Feedback Urgency & Sentiment",
            description = "Classify sentiment (POSITIVE/NEUTRAL/NEGATIVE) and urgency score (1-5) from short customer review.",
            complexity = ComplexityLevel.LOW,
            taskType = BenchmarkTaskType.SENTIMENT_ANALYSIS,
            systemInstruction = "You are a customer feedback evaluation assistant. Extract the exact sentiment and urgency rating.",
            inputData = "Text: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'",
            zeroShotPrompt = "Analyze the text and output sentiment (POSITIVE, NEUTRAL, or NEGATIVE) and urgency rating (1-5):\n\nText: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "Text: 'App crashed once while opening, but reopened fine on second try.'",
                    output = "{\"sentiment\": \"NEUTRAL\", \"urgency\": 2, \"reason\": \"Minor transient crash\"}"
                ),
                FewShotExample(
                    input = "Text: 'Loving the new Dark Mode update, great work team!'",
                    output = "{\"sentiment\": \"POSITIVE\", \"urgency\": 1, \"reason\": \"Praise feedback\"}"
                ),
                FewShotExample(
                    input = "Text: 'Cannot access my server data! Production is down immediately!'",
                    output = "{\"sentiment\": \"NEGATIVE\", \"urgency\": 5, \"reason\": \"Critical outage\"}"
                )
            ),
            fewShotPrompt = """Analyze the text and output JSON with sentiment, urgency (1-5), and reason.

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
            structuredPrompt = """Analyze the customer feedback text according to this strict JSON schema. Perform brief chain-of-thought analysis in reasoning before final classification.

Input: Text: 'My account was unexpectedly charged twice this morning and my subscription is now locked! I need this resolved before 5 PM.'""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "reasoning": { "type": "string" },
    "sentiment": { "type": "string", "enum": ["POSITIVE", "NEUTRAL", "NEGATIVE"] },
    "urgency": { "type": "integer", "minimum": 1, "maximum": 5 },
    "category": { "type": "string", "enum": ["BILLING", "TECHNICAL", "FEATURE", "GENERAL"] }
  },
  "required": ["reasoning", "sentiment", "urgency", "category"]
}""",
            expectedOutput = """{"sentiment": "NEGATIVE", "urgency": 5, "category": "BILLING"}""",
            groundTruthMap = mapOf("sentiment" to "NEGATIVE", "urgency" to "5", "category" to "BILLING")
        ),

        BenchmarkTestCase(
            id = "low_ner_extraction",
            title = "Named Entity & Date Disambiguation",
            description = "Extract person name, organization, event date, and location from an announcement snippet.",
            complexity = ComplexityLevel.LOW,
            taskType = BenchmarkTaskType.ENTITY_EXTRACTION,
            systemInstruction = "You are an entity extraction engine. Extract person, organization, date, and city precisely.",
            inputData = "Announcement: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'",
            zeroShotPrompt = "Extract the Person, Organization, Date (YYYY-MM-DD), and Location from this text:\n\n'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "'Marcus Vance of Apex Dynamics spoke on July 4, 2025 in Berlin.'",
                    output = "{\"person\": \"Marcus Vance\", \"organization\": \"Apex Dynamics\", \"date\": \"2025-07-04\", \"location\": \"Berlin\"}"
                ),
                FewShotExample(
                    input = "'Sarah Lin presented for BioTech Global in Tokyo on Jan 12, 2026.'",
                    output = "{\"person\": \"Sarah Lin\", \"organization\": \"BioTech Global\", \"date\": \"2026-01-12\", \"location\": \"Tokyo\"}"
                )
            ),
            fewShotPrompt = """Extract entities formatted as JSON.

Example 1:
Input: 'Marcus Vance of Apex Dynamics spoke on July 4, 2025 in Berlin.'
Output: {"person": "Marcus Vance", "organization": "Apex Dynamics", "date": "2025-07-04", "location": "Berlin"}

Example 2:
Input: 'Sarah Lin presented for BioTech Global in Tokyo on Jan 12, 2026.'
Output: {"person": "Sarah Lin", "organization": "BioTech Global", "date": "2026-01-12", "location": "Tokyo"}

Now extract:
Input: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'
Output:""",
            structuredPrompt = """Extract named entities into valid JSON conforming to the schema. Normalize date to ISO-8601 YYYY-MM-DD.

Input: 'Dr. Elena Rostova from NeoGen Labs announced on October 14, 2026 at the Zurich Tech Summit that clinical trials have begun.'""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "person": { "type": "string" },
    "organization": { "type": "string" },
    "date": { "type": "string", "pattern": "^\\d{4}-\\d{2}-\\d{2}$" },
    "location": { "type": "string" }
  },
  "required": ["person", "organization", "date", "location"]
}""",
            expectedOutput = """{"person": "Elena Rostova", "organization": "NeoGen Labs", "date": "2026-10-14", "location": "Zurich"}""",
            groundTruthMap = mapOf("person" to "Elena Rostova", "organization" to "NeoGen Labs", "date" to "2026-10-14", "location" to "Zurich")
        ),

        // ================= MEDIUM COMPLEXITY =================
        BenchmarkTestCase(
            id = "med_support_routing",
            title = "Multi-Field Support Ticket Routing",
            description = "Parse noisy user complaint, determine primary intent, severity (P1-P4), SLA hours, escalation team, and actionable refund flag.",
            complexity = ComplexityLevel.MEDIUM,
            taskType = BenchmarkTaskType.SUPPORT_TICKET_ROUTING,
            systemInstruction = "You are an automated Tier-3 enterprise ticket dispatch bot. Categorize inbound support tickets with strict operational metadata.",
            inputData = "Ticket #8941: 'We upgraded our Cloud API tier yesterday to Enterprise ($1,200/mo), but your OAuth endpoint is throwing 502 Bad Gateway for all 45 of our remote workers. We need rollback or fix within 2 hours or we demand a full refund and contract termination.'",
            zeroShotPrompt = "Parse this ticket and extract: Intent, Severity (P1/P2/P3/P4), SLA (hours), Escalation Queue, Refund Requested (true/false), and Summary:\n\nTicket #8941: 'We upgraded our Cloud API tier yesterday to Enterprise ($1,200/mo), but your OAuth endpoint is throwing 502 Bad Gateway for all 45 of our remote workers. We need rollback or fix within 2 hours or we demand a full refund and contract termination.'",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "Ticket #102: 'Forgot password reset email not arriving since 10 mins.'",
                    output = "{\"intent\": \"AUTH_ISSUE\", \"severity\": \"P3\", \"sla_hours\": 8, \"escalation_queue\": \"IDENTITY_OPS\", \"refund_requested\": false, \"estimated_affected_users\": 1}"
                ),
                FewShotExample(
                    input = "Ticket #103: 'Billing charged wrong card twice. Total $450. Please refund extra $225.'",
                    output = "{\"intent\": \"BILLING_DISPUTE\", \"severity\": \"P2\", \"sla_hours\": 4, \"escalation_queue\": \"BILLING_TIER2\", \"refund_requested\": true, \"estimated_affected_users\": 1}"
                )
            ),
            fewShotPrompt = """Parse support ticket into structured JSON schema matching team workflow.

Example 1:
Input: Ticket #102: 'Forgot password reset email not arriving since 10 mins.'
Output: {"intent": "AUTH_ISSUE", "severity": "P3", "sla_hours": 8, "escalation_queue": "IDENTITY_OPS", "refund_requested": false, "estimated_affected_users": 1}

Example 2:
Input: Ticket #103: 'Billing charged wrong card twice. Total $450. Please refund extra $225.'
Output: {"intent": "BILLING_DISPUTE", "severity": "P2", "sla_hours": 4, "escalation_queue": "BILLING_TIER2", "refund_requested": true, "estimated_affected_users": 1}

Now parse:
Input: Ticket #8941: 'We upgraded our Cloud API tier yesterday to Enterprise ($1,200/mo), but your OAuth endpoint is throwing 502 Bad Gateway for all 45 of our remote workers. We need rollback or fix within 2 hours or we demand a full refund and contract termination.'
Output:""",
            structuredPrompt = """Analyze the support ticket. First outline your reasoning for severity and routing queue, then produce strict JSON following the schema.

Ticket #8941: 'We upgraded our Cloud API tier yesterday to Enterprise ($1,200/mo), but your OAuth endpoint is throwing 502 Bad Gateway for all 45 of our remote workers. We need rollback or fix within 2 hours or we demand a full refund and contract termination.'""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "reasoning": { "type": "string" },
    "intent": { "type": "string", "enum": ["OUTAGE_AUTH", "BILLING_DISPUTE", "SECURITY_ALERT", "GENERAL_INQUIRY"] },
    "severity": { "type": "string", "enum": ["P1", "P2", "P3", "P4"] },
    "sla_hours": { "type": "integer" },
    "escalation_queue": { "type": "string", "enum": ["SECURITY_AUTH", "INFRA_CORE", "BILLING_TIER2", "GENERAL_SUPPORT"] },
    "refund_requested": { "type": "boolean" },
    "affected_users_count": { "type": "integer" }
  },
  "required": ["reasoning", "intent", "severity", "sla_hours", "escalation_queue", "refund_requested", "affected_users_count"]
}""",
            expectedOutput = """{"intent": "OUTAGE_AUTH", "severity": "P1", "sla_hours": 2, "escalation_queue": "INFRA_CORE", "refund_requested": true, "affected_users_count": 45}""",
            groundTruthMap = mapOf("intent" to "OUTAGE_AUTH", "severity" to "P1", "escalation_queue" to "INFRA_CORE", "refund_requested" to "true", "affected_users_count" to "45")
        ),

        BenchmarkTestCase(
            id = "med_financial_invoice",
            title = "Financial Transaction & Tax Deductibility",
            description = "Extract merchant, gross amount, currency, tax amount, tax rate percentage, business category, and tax deductible eligibility.",
            complexity = ComplexityLevel.MEDIUM,
            taskType = BenchmarkTaskType.FINANCIAL_AUDIT,
            systemInstruction = "You are a financial audit assistant. Extract exact accounting items and compute deductible status according to tax compliance rules.",
            inputData = "Receipt: 'Amazon Web Services Seattle WA. Invoice Date: Aug 12, 2026. Subtotal: $450.00. State VAT/Tax (8.5%): $38.25. Total Charged: $488.25 USD. Item: Dedicated EC2 Cloud Compute Node for Production Hosting.'",
            zeroShotPrompt = "Extract financial data from receipt: Merchant, Total, Currency, Tax Rate %, Category, and Deductible (Yes/No):\n\nReceipt: 'Amazon Web Services Seattle WA. Invoice Date: Aug 12, 2026. Subtotal: $450.00. State VAT/Tax (8.5%): $38.25. Total Charged: $488.25 USD. Item: Dedicated EC2 Cloud Compute Node for Production Hosting.'",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "Receipt: 'Starbucks Cafe #2910. 2x Caramel Macchiato. Total: $14.50 USD. Personal lunch.'",
                    output = "{\"merchant\": \"Starbucks\", \"gross_total\": 14.50, \"currency\": \"USD\", \"category\": \"MEALS_ENTERTAINMENT\", \"tax_deductible\": false}"
                ),
                FewShotExample(
                    input = "Receipt: 'JetBrains s.r.o. All Products IDE License annual. Subtotal: $249.00. Tax: $0.00. Total: $249.00 USD.'",
                    output = "{\"merchant\": \"JetBrains\", \"gross_total\": 249.00, \"currency\": \"USD\", \"category\": \"SOFTWARE_SUBSCRIPTION\", \"tax_deductible\": true}"
                )
            ),
            fewShotPrompt = """Extract transaction metadata into standardized accounting JSON.

Example 1:
Input: Receipt: 'Starbucks Cafe #2910. 2x Caramel Macchiato. Total: $14.50 USD. Personal lunch.'
Output: {"merchant": "Starbucks", "gross_total": 14.50, "currency": "USD", "category": "MEALS_ENTERTAINMENT", "tax_deductible": false}

Example 2:
Input: Receipt: 'JetBrains s.r.o. All Products IDE License annual. Subtotal: $249.00. Tax: $0.00. Total: $249.00 USD.'
Output: {"merchant": "JetBrains", "gross_total": 249.00, "currency": "USD", "category": "SOFTWARE_SUBSCRIPTION", "tax_deductible": true}

Now extract:
Input: Receipt: 'Amazon Web Services Seattle WA. Invoice Date: Aug 12, 2026. Subtotal: $450.00. State VAT/Tax (8.5%): $38.25. Total Charged: $488.25 USD. Item: Dedicated EC2 Cloud Compute Node for Production Hosting.'
Output:""",
            structuredPrompt = """Extract receipt data with strict schema validation. Categorize business purpose and tax deductibility.

Receipt: 'Amazon Web Services Seattle WA. Invoice Date: Aug 12, 2026. Subtotal: $450.00. State VAT/Tax (8.5%): $38.25. Total Charged: $488.25 USD. Item: Dedicated EC2 Cloud Compute Node for Production Hosting.'""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "merchant": { "type": "string" },
    "subtotal": { "type": "number" },
    "tax_amount": { "type": "number" },
    "gross_total": { "type": "number" },
    "currency": { "type": "string" },
    "tax_rate_percent": { "type": "number" },
    "category": { "type": "string", "enum": ["CLOUD_HOSTING", "SOFTWARE_SUBSCRIPTION", "OFFICE_SUPPLIES", "MEALS_ENTERTAINMENT"] },
    "tax_deductible": { "type": "boolean" }
  },
  "required": ["merchant", "subtotal", "tax_amount", "gross_total", "currency", "category", "tax_deductible"]
}""",
            expectedOutput = """{"merchant": "Amazon Web Services", "subtotal": 450.0, "tax_amount": 38.25, "gross_total": 488.25, "currency": "USD", "tax_rate_percent": 8.5, "category": "CLOUD_HOSTING", "tax_deductible": true}""",
            groundTruthMap = mapOf("merchant" to "Amazon Web Services", "gross_total" to "488.25", "category" to "CLOUD_HOSTING", "tax_deductible" to "true")
        ),

        // ================= HIGH COMPLEXITY =================
        BenchmarkTestCase(
            id = "high_logic_scheduler",
            title = "Multi-Constraint Logic Puzzle & Schedule Deduction",
            description = "Solve an intricate 5-agent meeting schedule with temporal, department, and room-capacity constraints.",
            complexity = ComplexityLevel.HIGH,
            taskType = BenchmarkTaskType.LOGICAL_REASONING,
            systemInstruction = "You are a formal constraint-satisfaction solver. You must determine the valid room and time slot for 4 simultaneous meetings satisfying all constraints.",
            inputData = """Constraints:
1. Team A (Engineering, 6 people) cannot meet before 11:00 AM and requires a Projector.
2. Team B (Design, 4 people) must meet at the same time or after Team A.
3. Team C (Legal, 3 people) must meet in Room 101 because of confidentiality.
4. Room 101 has 5 seats and no projector.
5. Room 201 has 10 seats and has a Projector.
6. Room 301 has 4 seats and no projector.
7. Available time slots: [09:00 AM, 11:00 AM, 02:00 PM].
Find the exact assignment for Team A, Team B, and Team C (Time slot & Room).""",
            zeroShotPrompt = """Solve this scheduling puzzle and output the exact time and room for Team A, Team B, and Team C:

Constraints:
1. Team A (Engineering, 6 people) cannot meet before 11:00 AM and requires a Projector.
2. Team B (Design, 4 people) must meet at the same time or after Team A.
3. Team C (Legal, 3 people) must meet in Room 101 because of confidentiality.
4. Room 101 has 5 seats and no projector.
5. Room 201 has 10 seats and has a Projector.
6. Room 301 has 4 seats and no projector.
7. Available time slots: [09:00 AM, 11:00 AM, 02:00 PM].
Assign each team a non-conflicting time and room.""",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "Constraints: 2 teams, X (needs Lab at 10 AM), Y (needs Office anytime). Slots: 10 AM, 2 PM. Rooms: Lab, Office.",
                    output = "{\"step_reasoning\": \"Team X needs Lab at 10 AM -> assigned Lab@10AM. Team Y takes remaining Office.\", \"assignments\": [{\"team\": \"X\", \"time\": \"10:00 AM\", \"room\": \"Lab\"}, {\"team\": \"Y\", \"time\": \"10:00 AM\", \"room\": \"Office\"}]}"
                )
            ),
            fewShotPrompt = """Solve multi-constraint logic puzzles with step-by-step reasoning followed by formal JSON assignment.

Example:
Input: Constraints: 2 teams, X (needs Lab at 10 AM), Y (needs Office anytime). Slots: 10 AM, 2 PM. Rooms: Lab, Office.
Output: {"step_reasoning": "Team X needs Lab at 10 AM -> assigned Lab@10AM. Team Y takes remaining Office.", "assignments": [{"team": "X", "time": "10:00 AM", "room": "Lab"}, {"team": "Y", "time": "10:00 AM", "room": "Office"}]}

Now solve:
Constraints:
1. Team A (Engineering, 6 people) cannot meet before 11:00 AM and requires a Projector.
2. Team B (Design, 4 people) must meet at the same time or after Team A.
3. Team C (Legal, 3 people) must meet in Room 101 because of confidentiality.
4. Room 101 has 5 seats and no projector.
5. Room 201 has 10 seats and has a Projector.
6. Room 301 has 4 seats and no projector.
7. Available time slots: [09:00 AM, 11:00 AM, 02:00 PM].
Output:""",
            structuredPrompt = """Solve the constraint satisfaction problem using Chain-of-Thought (CoT) step deduction. Emit valid JSON matching the schema with verified room capacity and constraint checks.

Constraints:
1. Team A (Engineering, 6 people) cannot meet before 11:00 AM and requires a Projector.
2. Team B (Design, 4 people) must meet at the same time or after Team A.
3. Team C (Legal, 3 people) must meet in Room 101 because of confidentiality.
4. Room 101 has 5 seats and no projector.
5. Room 201 has 10 seats and has a Projector.
6. Room 301 has 4 seats and no projector.
7. Available time slots: [09:00 AM, 11:00 AM, 02:00 PM].""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "chain_of_thought": { "type": "array", "items": { "type": "string" } },
    "team_a_room": { "type": "string", "enum": ["Room 201"] },
    "team_a_time": { "type": "string", "enum": ["11:00 AM", "02:00 PM"] },
    "team_c_room": { "type": "string", "enum": ["Room 101"] },
    "all_constraints_satisfied": { "type": "boolean" }
  },
  "required": ["chain_of_thought", "team_a_room", "team_a_time", "team_c_room", "all_constraints_satisfied"]
}""",
            expectedOutput = """{"team_a_room": "Room 201", "team_c_room": "Room 101", "all_constraints_satisfied": true}""",
            groundTruthMap = mapOf("team_a_room" to "Room 201", "team_c_room" to "Room 101", "all_constraints_satisfied" to "true")
        ),

        BenchmarkTestCase(
            id = "high_code_vulnerability",
            title = "Python Security Vulnerability & CWE Patch",
            description = "Identify critical CWE security flaws in Python code, extract line numbers, severity score (CVSS), and generate verified mitigation patch.",
            complexity = ComplexityLevel.HIGH,
            taskType = BenchmarkTaskType.CODE_DEBUGGING,
            systemInstruction = "You are a static application security testing (SAST) cybersecurity analyst. Identify Common Weakness Enumeration (CWE) codes and generate strict remediation patches.",
            inputData = """def query_user_profile(db_cursor, username, user_role):
    # Fetch user data from SQLite database
    query = "SELECT id, email, secret_token FROM users WHERE username = '" + username + "' AND role = '" + user_role + "'"
    db_cursor.execute(query)
    return db_cursor.fetchall()""",
            zeroShotPrompt = """Analyze this Python code for security vulnerabilities. State CWE ID, Severity, Affected Line Number, and provide fixed code:

def query_user_profile(db_cursor, username, user_role):
    # Fetch user data from SQLite database
    query = "SELECT id, email, secret_token FROM users WHERE username = '" + username + "' AND role = '" + user_role + "'"
    db_cursor.execute(query)
    return db_cursor.fetchall()""",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "import os\ndef ping(host):\n    return os.system('ping -c 1 ' + host)",
                    output = "{\"cwe_id\": \"CWE-78\", \"cwe_title\": \"OS Command Injection\", \"cvss_score\": 9.8, \"vulnerable_line\": 3, \"fix\": \"import subprocess\\ndef ping(host):\\n    return subprocess.run(['ping', '-c', '1', host])\"}"
                )
            ),
            fewShotPrompt = """Analyze Python code for security flaws and output strictly in JSON format.

Example 1:
Input: import os\ndef ping(host):\n    return os.system('ping -c 1 ' + host)
Output: {"cwe_id": "CWE-78", "cwe_title": "OS Command Injection", "cvss_score": 9.8, "vulnerable_line": 3, "fix": "import subprocess\ndef ping(host):\n    return subprocess.run(['ping', '-c', '1', host])"}

Now analyze:
Input:
def query_user_profile(db_cursor, username, user_role):
    # Fetch user data from SQLite database
    query = "SELECT id, email, secret_token FROM users WHERE username = '" + username + "' AND role = '" + user_role + "'"
    db_cursor.execute(query)
    return db_cursor.fetchall()
Output:""",
            structuredPrompt = """Perform security code review and output JSON conforming strictly to the schema. Include security reasoning and parameterized query mitigation.

def query_user_profile(db_cursor, username, user_role):
    # Fetch user data from SQLite database
    query = "SELECT id, email, secret_token FROM users WHERE username = '" + username + "' AND role = '" + user_role + "'"
    db_cursor.execute(query)
    return db_cursor.fetchall()""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "vulnerability_analysis": { "type": "string" },
    "cwe_id": { "type": "string", "enum": ["CWE-89", "CWE-78", "CWE-79"] },
    "cwe_title": { "type": "string" },
    "cvss_score": { "type": "number", "minimum": 0.0, "maximum": 10.0 },
    "vulnerable_line": { "type": "integer" },
    "mitigation_approach": { "type": "string", "enum": ["PARAMETERIZED_QUERIES", "SANITIZATION", "TYPE_CASTING"] },
    "remediated_code": { "type": "string" }
  },
  "required": ["vulnerability_analysis", "cwe_id", "cvss_score", "vulnerable_line", "mitigation_approach", "remediated_code"]
}""",
            expectedOutput = """{"cwe_id": "CWE-89", "cwe_title": "Improper Neutralization of Special Elements used in an SQL Command (SQL Injection)", "cvss_score": 9.8, "vulnerable_line": 3, "mitigation_approach": "PARAMETERIZED_QUERIES"}""",
            groundTruthMap = mapOf("cwe_id" to "CWE-89", "vulnerable_line" to "3", "mitigation_approach" to "PARAMETERIZED_QUERIES")
        ),

        BenchmarkTestCase(
            id = "high_compliance_matrix",
            title = "Enterprise Privacy & GDPR Cross-Border Matrix",
            description = "Extract legal basis, data subject category, international transfer safeguards (SCC/BCR), and DPO notification requirement from a data processing agreement snippet.",
            complexity = ComplexityLevel.HIGH,
            taskType = BenchmarkTaskType.COMPLIANCE_EXTRACTION,
            systemInstruction = "You are a legal privacy compliance auditor. Verify GDPR Article 6 legal basis and Chapter V international transfer adequacy requirements.",
            inputData = "DPA Section 4.2: 'Customer biometric identifiers (fingerprint telemetry) collected in Frankfurt EU datacenter will be mirrored to Sydney, Australia for secondary real-time disaster failover without an adequacy decision. Processing is strictly to fulfill SaaS maintenance contracts.'",
            zeroShotPrompt = "Extract legal compliance details: Legal Basis (GDPR Art 6), Data Type, International Transfer Risk, Standard Contractual Clauses Needed (Yes/No), and Breach SLA:\n\n'Customer biometric identifiers (fingerprint telemetry) collected in Frankfurt EU datacenter will be mirrored to Sydney, Australia for secondary real-time disaster failover without an adequacy decision. Processing is strictly to fulfill SaaS maintenance contracts.'",
            fewShotExamples = listOf(
                FewShotExample(
                    input = "'Employee payroll addresses in London sent to UK payroll processor.'",
                    output = "{\"special_category_data\": false, \"gdpr_article\": \"Article 6(1)(b) Contractual\", \"transfer_outside_eea\": false, \"scc_mandatory\": false}"
                )
            ),
            fewShotPrompt = """Extract GDPR compliance obligations into structured audit JSON.

Example:
Input: 'Employee payroll addresses in London sent to UK payroll processor.'
Output: {"special_category_data": false, "gdpr_article": "Article 6(1)(b) Contractual", "transfer_outside_eea": false, "scc_mandatory": false}

Now extract:
Input: 'Customer biometric identifiers (fingerprint telemetry) collected in Frankfurt EU datacenter will be mirrored to Sydney, Australia for secondary real-time disaster failover without an adequacy decision. Processing is strictly to fulfill SaaS maintenance contracts.'
Output:""",
            structuredPrompt = """Audit the data transfer agreement against GDPR compliance rules. Output structured JSON matching the schema with compliance risk reasoning.

DPA Section 4.2: 'Customer biometric identifiers (fingerprint telemetry) collected in Frankfurt EU datacenter will be mirrored to Sydney, Australia for secondary real-time disaster failover without an adequacy decision. Processing is strictly to fulfill SaaS maintenance contracts.'""",
            jsonSchema = """{
  "type": "object",
  "properties": {
    "audit_notes": { "type": "string" },
    "is_special_category_data": { "type": "boolean" },
    "data_type": { "type": "string", "enum": ["BIOMETRIC", "HEALTH", "FINANCIAL", "STANDARD_PII"] },
    "transfer_outside_eea": { "type": "boolean" },
    "transfer_mechanism_required": { "type": "string", "enum": ["STANDARD_CONTRACTUAL_CLAUSES_SCC", "BINDING_CORPORATE_RULES_BCR", "ADEQUACY_DECISION", "NONE"] },
    "compliance_risk_level": { "type": "string", "enum": ["CRITICAL", "HIGH", "MEDIUM", "LOW"] }
  },
  "required": ["audit_notes", "is_special_category_data", "data_type", "transfer_outside_eea", "transfer_mechanism_required", "compliance_risk_level"]
}""",
            expectedOutput = """{"is_special_category_data": true, "data_type": "BIOMETRIC", "transfer_outside_eea": true, "transfer_mechanism_required": "STANDARD_CONTRACTUAL_CLAUSES_SCC", "compliance_risk_level": "CRITICAL"}""",
            groundTruthMap = mapOf("is_special_category_data" to "true", "data_type" to "BIOMETRIC", "transfer_outside_eea" to "true", "transfer_mechanism_required" to "STANDARD_CONTRACTUAL_CLAUSES_SCC", "compliance_risk_level" to "CRITICAL")
        )
    )
}
