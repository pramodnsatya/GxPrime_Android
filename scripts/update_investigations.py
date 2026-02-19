#!/usr/bin/env python3
"""
Bulk update Quality Unit questions in QualityUnitQuestions.kt with PDF content
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Investigations (1.2) - Replace all 25 questions
investigations_replacements = [
    (r'Question\("qu_investigations_2".*?\),', 'Question("qu_investigations_2", "qu_investigations", "Does the investigation SOP mandate use of structured root cause analysis (RCA) tools such as 5-Why (minimum 5 levels), Fishbone/Ishikawa diagram, or Fault Tree Analysis for all major/critical investigations?", 2),'),
    (r'Question\("qu_investigations_3".*?\),', 'Question("qu_investigations_3", "qu_investigations", "Are investigations for OOS, deviations, and complaints initiated promptly (within 24h of detection) by QA personnel independent of the originating department, with automatic quarantine of affected material?", 3),'),
    (r'Question\("qu_investigations_4".*?\),', 'Question("qu_investigations_4", "qu_investigations", "Are investigation hypotheses (e.g., lab error, process variation, equipment failure) scientifically sound, documented in a predefined protocol, and tested with evidence (data, literature references)?", 4),'),
    (r'Question\("qu_investigations_5".*?\),', 'Question("qu_investigations_5", "qu_investigations", "Does QA checklist ensure investigations systematically address all potential failure modes (man, machine, method, material, measurement, environment) with none applicable explicitly justified?", 5),'),
    (r'Question\("qu_investigations_6".*?\),', 'Question("qu_investigations_6", "qu_investigations", "Are investigation leads trained (initial and annual refresher) and qualified (successful mock investigations ≥90%) as independent from routine operations, with current certification matrix?", 6),'),
    (r'Question\("qu_investigations_7".*?\),', 'Question("qu_investigations_7", "qu_investigations", "When laboratory OOS is confirmed manufacturing-related (Phase 2), are investigations integrated with production records review, cross-functional input, and batch disposition recommendation?", 7),'),
    (r'Question\("qu_investigations_8".*?\),', 'Question("qu_investigations_8", "qu_investigations", "Do current investigations include mandatory review of previous similar events (last 24 months) to prevent repeated root causes, with linkages or escalations documented?", 8),'),
    (r'Question\("qu_investigations_9".*?\),', 'Question("qu_investigations_9", "qu_investigations", "Before QA approval, are the investigation bodies peer-reviewed for completeness (scope, data, rationale, conclusions) using a standardized checklist covering all SOP requirements?", 9),'),
    (r'Question\("qu_investigations_10".*?\),', 'Question("qu_investigations_10", "qu_investigations", "Are operator/staff interview notes recorded contemporaneously during investigations, verbatim where possible, signed/dated by interviewee, and attached to the investigation report?", 10),'),
    (r'Question\("qu_investigations_11".*?\),', 'Question("qu_investigations_11", "qu_investigations", "When investigations uncover undocumented practices (e.g., verbal instructions, unapproved workarounds), are they escalated as separate deviations or CAPAs with immediate interim controls?", 11),'),
    (r'Question\("qu_investigations_12".*?\),', 'Question("qu_investigations_12", "qu_investigations", "For product/process investigations, are environmental (EM trends), equipment (maintenance logs, calibration), and utility data systematically reviewed and correlated with event timing?", 12),'),
    (r'Question\("qu_investigations_13".*?\),', 'Question("qu_investigations_13", "qu_investigations", "Are failures attributed to human error supported by objective evidence (training records, qualification status, observation videos, multiple analysts affected) rather than default assumption?", 13),'),
    (r'Question\("qu_investigations_14".*?\),', 'Question("qu_investigations_14", "qu_investigations", "Are investigation reports complete with all supporting attachments (equipment logs, chromatograms, EM data, pictures) traceable to time/location and reviewed for relevance?", 14),'),
    (r'Question\("qu_investigations_15".*?\),', 'Question("qu_investigations_15", "qu_investigations", "Are no assignable cause conclusions scientifically justified with evidence of exhaustive investigation (all failure modes ruled out, statistical analysis confirming abnormality) per FDA OOS guidance?", 15),'),
    (r'Question\("qu_investigations_16".*?\),', 'Question("qu_investigations_16", "qu_investigations", "Does QA reject and return investigations lacking evidence-based conclusions (e.g., generic training needed, unsubstantiated root causes) with documented reasons for rework?", 16),'),
    (r'Question\("qu_investigations_17".*?\),', 'Question("qu_investigations_17", "qu_investigations", "Are investigation timelines tracked with justifications for extensions QA-approved and overdue aging report reviewed monthly?", 17),'),
    (r'Question\("qu_investigations_18".*?\),', 'Question("qu_investigations_18", "qu_investigations", "Are closed investigations reopened when new evidence emerges (e.g., audit finding, complaint correlation) within defined criteria (e.g., within 1 year of closure) per SOP?", 18),'),
    (r'Question\("qu_investigations_19".*?\),', 'Question("qu_investigations_19", "qu_investigations", "Is impact assessment for distributed batches (e.g., recall calculation, stability extrapolation, patient risk) documented with decision tree for field alert/reporting?", 19),'),
    (r'Question\("qu_investigations_20".*?\),', 'Question("qu_investigations_20", "qu_investigations", "Does the site maintain an investigation knowledge repository/database (e.g., lessons learned database, searchable QMS module) accessible to investigators with annual update requirement?", 20),'),
    (r'Question\("qu_investigations_21".*?\),', 'Question("qu_investigations_21", "qu_investigations", "When investigations identify systemic issues (recurring, multi-batch), are formal risk assessments (FMEA RPN>100) included with prioritized CAPA recommendations?", 21),'),
    (r'Question\("qu_investigations_22".*?\),', 'Question("qu_investigations_22", "qu_investigations", "Does QA verify implementation and effectiveness of interim control measures (e.g., additional checks, 100% inspection) defined during open investigations before closure?", 22),'),
    (r'Question\("qu_investigations_23".*?\),', 'Question("qu_investigations_23", "qu_investigations", "Are rejected/failed batches systematically linked to their root cause investigations with cross-references in batch records and APR for trend analysis?", 23),'),
    (r'Question\("qu_investigations_24".*?\),', 'Question("qu_investigations_24", "qu_investigations", "Are investigation findings from corporate/sister sites leveraged through shared knowledge portals, with relevant lessons incorporated into local CAPA or training plans?", 24),'),
    (r'Question\("qu_investigations_25".*?\),', 'Question("qu_investigations_25", "qu_investigations", "When applicable (OOS, complaints), are witness samples, retains, or duplicates included/analyzed in investigations with documented storage conditions and chain-of-custody?", 25),'),
]

for pattern, replacement in investigations_replacements:
    content = re.sub(pattern, replacement, content, count=1)

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated Investigations questions successfully")
