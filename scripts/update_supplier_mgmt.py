#!/usr/bin/env python3
"""
Update Supplier Quality Oversight and Management Review
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Supplier Quality Oversight (1.18)
supplier_questions = [
    ('qu_supplier_1', 'Is there an approved Standard Operating Procedure (SOP) that defines supplier qualification through questionnaires, audits, and testing; approval based on scorecards achieving at least 85%; and ongoing monitoring through annual reviews with scores maintained at or above 80%?'),
    ('qu_supplier_2', 'Are suppliers categorized by material criticality using a defined matrix — for example, critical suppliers of Active Pharmaceutical Ingredients (APIs) or key excipients audited annually; medium-risk excipient suppliers audited every two years; and low-risk packaging suppliers audited every three years?'),
    ('qu_supplier_3', 'Are supplier audits performed according to risk ranking — for example, critical suppliers audited at least annually with scores ≥90, and on-site audits conducted when red flags are identified — with adherence to the audit schedule maintained at ≥95%?'),
    ('qu_supplier_4', 'Are supplier Certificates of Analysis (CoAs) verified against in-house testing — for example, 100% identity testing, assay and impurity testing performed on a skip-lot basis for low-risk materials — according to an approved reduced testing plan?'),
    ('qu_supplier_5', 'Are material specifications communicated clearly to suppliers through approved technical packages that include monographs, tolerances, and Good Manufacturing Practice (GMP) requirements?'),
    ('qu_supplier_6', 'Are supplier change notifications — such as changes in site, process, or API source — evaluated under site change control procedures, with comparability data required to support approval?'),
    ('qu_supplier_7', 'Are suppliers monitored quarterly for performance metrics such as on-time delivery ≥98%, right-first-time quality ≥99%, and compliance with quality standards ensuring out-of-specification (OOS) results remain below 1%?'),
    ('qu_supplier_8', 'Are supplier performance scorecards maintained, incorporating audit results, on-time delivery (OTD), and quality scores, presented in weighted formats with red/amber/green color coding, and reviewed semi-annually?'),
    ('qu_supplier_9', 'Are new suppliers qualified through full assessments including 100% questionnaire completion, audits, full testing of the first three lots, and verification of GMP certification?'),
    ('qu_supplier_10', 'Are material complaints investigated jointly with suppliers, including root cause analysis and shared Corrective and Preventive Actions (CAPAs), particularly for critical suppliers?'),
    ('qu_supplier_11', 'Are suppliers required to provide lot traceability documentation — including Certificates of Analysis (CoA), Material of Construction (MoC) records, and batch genealogy trees — covering at least three generations back?'),
    ('qu_supplier_12', 'Are supplier GMP certificates and regulatory inspection results — such as U.S. Food and Drug Administration (FDA) or European Medicines Agency (EMA) Form 483 observations — reviewed annually, with contractual rights to audit included?'),
    ('qu_supplier_13', 'Are supplier Corrective and Preventive Actions (CAPAs) reviewed for adequacy, ensuring closure within 90 days, effectiveness supported by data, and recurrence prevention before acceptance?'),
    ('qu_supplier_14', 'Are fraudulent or suspect materials — such as tampered seals or mismatched Certificates of Analysis (CoAs) — escalated to Quality Assurance (QA), Regulatory Affairs (RA), and regulators within 24 hours, with immediate quarantine applied?'),
    ('qu_supplier_15', 'Are alternate or backup suppliers evaluated for risk, with parallel qualification and dual sourcing implemented for critical APIs covering at least 50% of supply?'),
    ('qu_supplier_16', 'Are raw material testing frequencies justified through risk-based SOPs — for example, skip-lot testing permitted for low-risk materials with ≥6 months of stability data, while identity testing remains at 100%?'),
    ('qu_supplier_17', 'Are Technical and Quality Agreements maintained and updated annually for critical suppliers, covering specifications, change management, audit requirements, and rejection authority?'),
    ('qu_supplier_18', 'Are audit findings communicated clearly to suppliers, with rated reports issued within 14 days and CAPA responses required within 30 days, tracked to closure?'),
    ('qu_supplier_19', 'Are supplier-related trends — such as out-of-specification (OOS) results or late deliveries — included in product Annual Product Quality Reviews (APQRs), with preventive actions documented?'),
    ('qu_supplier_20', 'Are supplier risks — such as single-source dependency or geopolitical concerns — included in the site risk register, with Failure Mode and Effects Analysis (FMEA) updated quarterly?'),
    ('qu_supplier_21', 'Are transportation and logistics partners qualified through Good Distribution Practice (GDP) audits, temperature validation studies confirming 2–8°C control for 96 hours, and contingency planning?'),
    ('qu_supplier_22', 'Are packaging supplier controls reviewed, including ink migration testing (<10 parts per billion), laminate delamination testing, and foil pinhole testing with Acceptance Quality Limit (AQL) of 0.65?'),
    ('qu_supplier_23', 'Are cold chain suppliers qualified based on thermal mapping, using validated shippers that maintain 2–8°C, with data loggers calibrated to ±0.5°C?'),
    ('qu_supplier_24', 'Are material quarantine rules applied to supplier failures — such as non-conforming Certificates of Analysis (CoAs) or audit red flags — until issues are resolved?'),
    ('qu_supplier_25', 'Are supplier Key Performance Indicators (KPIs) — such as on-time delivery ≥98% and quality compliance ≥99% — reviewed quarterly during management reviews, with delisting actions taken when performance falls below thresholds?'),
]

for i, (q_id, q_text) in enumerate(supplier_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_supplier", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Supplier Quality questions")

# Management Review & Quality Metrics (1.17)
mgmt_review_questions = [
    ('qu_mgmt_review_1', 'Is there an approved Standard Operating Procedure (SOP) that defines management review requirements, including agenda, attendees, frequency, inputs aligned with International Council for Harmonisation (ICH) guideline Q10, and action tracking?'),
    ('qu_mgmt_review_2', 'Are management reviews conducted on a defined schedule — for example, monthly for operations, quarterly for quality, and annually for strategic reviews — with at least 95% adherence to the schedule?'),
    ('qu_mgmt_review_3', 'Are quality metrics defined with clear calculation criteria, such as Out-of-Specification (OOS) rate calculated as OOS batches divided by total batches ×100, with targets set at less than 1%, and data sources documented in the Key Performance Indicator (KPI) library?'),
    ('qu_mgmt_review_4', 'Are deviations tracked by count and trend, Corrective and Preventive Actions (CAPAs) monitored to ensure overdue cases remain below 5% and effectiveness is at least 90%, complaints measured by rate per million units, and OOS trends graphed and discussed?'),
    ('qu_mgmt_review_5', 'Are internal and external audit findings, including closure rates, and regulatory observations such as U.S. Food and Drug Administration (FDA) Form 483 responses, reviewed with current status updates?'),
    ('qu_mgmt_review_6', 'Are KPI dashboards, such as real-time systems built in Power BI or Tableau, used for decision-making and covering at least 20 quality metrics?'),
    ('qu_mgmt_review_7', 'Are recurring failures — defined as the same cause occurring three or more times within 12 months — highlighted with management action items assigned and escalated?'),
    ('qu_mgmt_review_8', 'Are resource needs assessed, including staffing ratios such as one Quality Assurance (QA) staff per eight production staff, training budgets, and equipment capital expenditure (CAPEX), with identified gaps addressed?'),
    ('qu_mgmt_review_9', 'Are process capability metrics, such as process capability index (Cpk) and process performance index (Ppk) maintained at ≥1.33 for critical quality attributes (CQAs), reviewed using control charts for commercial products?'),
    ('qu_mgmt_review_10', 'Are supplier performance indicators included in scorecards, such as on-time delivery ≥98%, OOS results <2%, and audit scores ≥90%?'),
    ('qu_mgmt_review_11', 'Are in-process controls (IPC), such as yield and pH trends, and process control trends, such as Statistical Process Control (SPC) alarms, reviewed for stability?'),
    ('qu_mgmt_review_12', 'Are product recalls and returns assessed for rate, root causes, and CAPA effectiveness, with preventive measures implemented?'),
    ('qu_mgmt_review_13', 'Are quality risks reviewed and updated, including the top 10 risks in the Failure Mode and Effects Analysis (FMEA) register and residual Risk Priority Numbers (RPNs), with new mitigations applied?'),
    ('qu_mgmt_review_14', 'Are market issues, such as shortages or competitor recalls impacting supply, escalated to top management with contingency plans?'),
    ('qu_mgmt_review_15', 'Are improvement plans developed from Annual Product Quality Reviews (APQRs) and audits, tracked for completion ≥90%, with owners and dates documented in the Quality Management System (QMS)?'),
    ('qu_mgmt_review_16', 'Are environmental monitoring results reviewed, such as Grade A cleanroom limits of fewer than 1 colony-forming unit (CFU), and utilities monitored, such as Water for Injection (WFI) conductivity maintained below 1.3 microsiemens (μS), with excursions tracked?'),
    ('qu_mgmt_review_17', 'Are Quality Control (QC) laboratory metrics reviewed, such as test turnaround time (TAT) ≤48 hours, analyst proficiency ≥95%, and instrument uptime ≥98%?'),
    ('qu_mgmt_review_18', 'Are on-time batch release metrics assessed, such as ≥95% of batches released within five days post-production, with bottlenecks identified?'),
    ('qu_mgmt_review_19', 'Is production capacity utilization analyzed against quality issues, ensuring downtime due to quality problems remains below 10%?'),
    ('qu_mgmt_review_20', 'Are trends in deviation misclassification or downgrading reviewed, ensuring audit findings remain below 5%, with training and CAPAs applied?'),
    ('qu_mgmt_review_21', 'Is training performance reviewed, ensuring completion rates ≥98% and overdue training <2%, with gaps addressed through action plans?'),
    ('qu_mgmt_review_22', 'Is CAPA effectiveness examined, ensuring recurrence rates <5% and on-time closure ≥95%, with underperforming CAPAs escalated?'),
    ('qu_mgmt_review_23', 'Are regulatory commitments, such as inspection responses and product variations, reviewed for status and timelines?'),
    ('qu_mgmt_review_24', 'Are cross-functional departments — including production, Quality Control (QC), Regulatory Affairs (RA), and supply chain — included in management review meetings, with attendance ≥80%?'),
    ('qu_mgmt_review_25', 'Are meeting minutes recorded, including attendees, metrics reviewed, decisions made, and action owners with dates, distributed within seven days, and followed up with ≥90% completion?'),
]

for i, (q_id, q_text) in enumerate(mgmt_review_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_mgmt_review", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Management Review questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating Supplier Quality and Management Review")
