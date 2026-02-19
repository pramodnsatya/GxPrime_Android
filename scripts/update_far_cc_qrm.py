#!/usr/bin/env python3
"""
Update Field Alert Reports, Change Control, Quality Risk Management
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Field Alert Reports (1.9)
far_questions = [
    ('qu_field_alerts_1', 'Is there an approved FAR SOP explicitly aligned with 21 CFR 314.81(b)(1), defining submission process (Form FDA 3331a, 3 working days), responsible parties, and escalation?'),
    ('qu_field_alerts_2', 'Are potential FAR triggers clearly defined in SOP (OOS distributed batches, contamination, mix-ups, sterility failures, labeling errors, deterioration) with decision tree/matrix?'),
    ('qu_field_alerts_3', 'Are FAR assessments initiated within the 3-working-day reporting window upon information receipt (e.g., OOS confirmation, complaint), with log timestamp ≤72h?'),
    ('qu_field_alerts_4', 'Are responsible FAR coordinators (RA/QA leads) clearly assigned by site/NDA with contact lists, backup coverage, and annual training completion ≥100%?'),
    ('qu_field_alerts_5', 'Are all quality complaints reviewed for FAR relevance (e.g., potency failure, foreign matter) during triage, with documented FAR yes/no decision ≤24h?'),
    ('qu_field_alerts_6', 'Are stability OOS/OOT failures (distributed shelf-life batches) evaluated for FAR impact, including market withdrawal risk, within 3 days?'),
    ('qu_field_alerts_7', 'Are batch discrepancies (yield variance >5%, potency <95%) assessed for market risk requiring FAR, with retain testing and distribution trace completed?'),
    ('qu_field_alerts_8', 'Are distributed batch OOS/OOT lab results assessed for FAR triggers unless scientifically invalidated (lab error confirmed) within 3 working days?'),
    ('qu_field_alerts_9', 'Are manufacturing deviations (aseptic breach, equipment failure) evaluated for FAR implications on distributed product quality/safety?'),
    ('qu_field_alerts_10', 'Are critical packaging failures (seal integrity loss, tampering evidence) included in FAR evaluation for distributed lots, with visual/physical testing?'),
    ('qu_field_alerts_11', 'Is documentation supporting FAR no submission or submit decisions retained (risk assessment, retain analysis, distribution data) for ≥5 years?'),
    ('qu_field_alerts_12', 'Are retain samples examined (visual, assay, sterility where applicable) for all FAR assessments, with results attached to decision record?'),
    ('qu_field_alerts_13', 'Are product quality complaints (color change, tablet breakage) systematically reviewed against FAR criteria (spec failure, contamination) during investigation?'),
    ('qu_field_alerts_14', 'Are FARs communicated internally to corporate RA and externally to FDA district office promptly (initial ≤3 days, follow-up ≤15 days) via Form FDA 3331a?'),
    ('qu_field_alerts_15', 'Are cross-functional teams (QA, RA, production, QC) involved in FAR assessments for complex triggers (sterility, systemic deviations)?'),
    ('qu_field_alerts_16', 'Are FARs trended quarterly (by trigger type, product, site) to identify systemic issues, with Pareto analysis in Quality Management Review?'),
    ('qu_field_alerts_17', 'Is market withdrawal/recall risk evaluated explicitly in every FAR assessment, with decision documented (e.g., FAR + recall evaluation)?'),
    ('qu_field_alerts_18', 'Are FAR-related CAPAs (root cause from investigation) created/tracked to closure, with effectiveness verification ≤6 months?'),
    ('qu_field_alerts_19', 'Are FAR submit/no-submit decisions justified with documented risk assessments (patient safety, product quality impact, distribution scope)?'),
    ('qu_field_alerts_20', 'Are timelines for FAR submission strictly monitored (initial ≤3 working days, follow-up per investigation), with aging report/escalation for delays?'),
    ('qu_field_alerts_21', 'Are regulatory commitments from FARs (e.g., enhanced testing, process validation) tracked to closure via QMS with verification evidence?'),
    ('qu_field_alerts_22', 'Are personnel involved in FAR (coordinators, investigators) trained annually on SOP/21 CFR 314.81(b)(1) with competency quiz ≥90% pass rate?'),
    ('qu_field_alerts_23', 'Are FARs (submitted + assessed) included in management review discussions quarterly, with trends and CAPA status reported?'),
    ('qu_field_alerts_24', 'Are near-miss events meeting FAR criteria if escalated (e.g., potential mix-up averted) monitored/trended, even if no submission required?'),
    ('qu_field_alerts_25', 'Are drug shortage implications (e.g., quality deviation during shortage) assessed during FAR decisions, with allocation risk documented?'),
]

for i, (q_id, q_text) in enumerate(far_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_field_alerts", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Field Alert Reports questions")

# Change Control (1.10)
change_control_questions = [
    ('qu_change_control_1', 'Is there an approved SOP defining full change control lifecycle: initiation (form submission), impact assessment (QRM), approval (multi-level), implementation (work orders), closure (verification)?'),
    ('qu_change_control_2', 'Are changes categorized as minor (no validation), major (validation required), critical (regulatory filing) with clear definitions, examples, and decision matrix in SOP?'),
    ('qu_change_control_3', 'Are all GxP changes initiated via formal change control record prior to implementation, with 100% compliance verified by retrospective audits?'),
    ('qu_change_control_4', 'Are cross-functional evaluations (QA, Production, QC, Engineering, RA) documented for each change via sign-off matrix or meeting minutes?'),
    ('qu_change_control_5', 'Is Quality Risk Management (FMEA/PHA, RPN calculated) included for every change to evaluate product/process impact pre-approval?'),
    ('qu_change_control_6', 'Is regulatory filing impact (CBE, PAS, annual report) assessed/documented by RA for changes affecting NDA/ANDA specs/process, with submission tracked?'),
    ('qu_change_control_7', 'Are validation/qualification requirements (IQ/OQ/PQ, cleaning verification, stability) determined/documented as part of change control, executed pre-closure?'),
    ('qu_change_control_8', 'Are changes linked to relevant CAPAs/deviations/OOS with cross-references when originating from quality events?'),
    ('qu_change_control_9', 'Are temporary changes (≤90 days trials) controlled via separate procedure with risk assessment, monitoring plan, and retrospective full review?'),
    ('qu_change_control_10', 'Are changes impacting product/method specs reviewed/approved by Quality and Regulatory before implementation?'),
    ('qu_change_control_11', 'Are equipment/facility changes evaluated for re-qualification status (partial/full IQ/OQ/PQ) based on risk assessment?'),
    ('qu_change_control_12', 'Are software/system changes evaluated under Computerized System Validation (CSV) requirements (GAMP 5 risk category, testing plan)?'),
    ('qu_change_control_13', 'Are supplier-related changes (API/excipient site switch) assessed for material impact via comparability protocol and stability?'),
    ('qu_change_control_14', 'Are effectiveness checks defined/executed for major/critical changes (KPIs met, post-change data stable 6 months) before closure?'),
    ('qu_change_control_15', 'Are change control timelines risk-based/monitored (critical ≤60 days, major ≤90 days) via dashboard with overdue escalations?'),
    ('qu_change_control_16', 'Are change approvals documented with individual justifications (risk acceptance, mitigations) from each approver?'),
    ('qu_change_control_17', 'Are emergency changes (unplanned urgent fixes) controlled retrospectively (within 7 days full review/risk assessment/approval) per SOP?'),
    ('qu_change_control_18', 'Are changes to cleaning processes evaluated for cross-contamination risk (MAC/PDE recalculation, swab verification)?'),
    ('qu_change_control_19', 'Are change history records complete/traceable (what/who/when/rationale) for ≥5 years, audit trail compliant?'),
    ('qu_change_control_20', 'Are training requirements (affected personnel) defined/completed (≥95% trained) before change implementation?'),
    ('qu_change_control_21', 'Are changes to batch records (MPR/BMR templates) reviewed/verified by QA/production before use in production?'),
    ('qu_change_control_22', 'Are packaging/artwork changes tracked/verified (mock-ups, barcode validation, supplier proofs) pre-implementation?'),
    ('qu_change_control_23', 'Are changes evaluated for impact on ongoing/in-process batches, with hold/quarantine if required?'),
    ('qu_change_control_24', 'Are post-implementation reviews performed/documented (6 months data, KPIs stable) for major changes before closure?'),
    ('qu_change_control_25', 'Are change control metrics (cycle time, overdue %, CAPA linkage) reviewed quarterly in management review with improvement actions?'),
]

for i, (q_id, q_text) in enumerate(change_control_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_change_control", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Change Control questions")

# Quality Risk Management (1.6)
risk_mgmt_questions = [
    ('qu_risk_mgmt_1', 'Is there a QRM SOP explicitly aligned with ICH Q9(R1) principles (risk assessment/control/review/communication) and FDA expectations, defining roles, tools (FMEA/HACCP), and documentation standards?'),
    ('qu_risk_mgmt_2', 'Are risk assessments performed using consistent, site-standardized tools (FMEA with RPN, HACCP CCPs, PHA hazard lists) per approved templates, with training required for facilitators?'),
    ('qu_risk_mgmt_3', 'Are risk assessments conducted proactively for new processes/equipment (before implementation) and not solely reactively after deviations, with ≥80% proactive per annual QRM report?'),
    ('qu_risk_mgmt_4', 'Are severity (patient harm scale 1-10), occurrence (frequency 1-10), and detectability (control strength 1-10) clearly defined with site-specific justification tables and examples?'),
    ('qu_risk_mgmt_5', 'Are high-risk items (e.g., FMEA RPN>100, HACCP high severity) automatically escalated for mitigation actions with QA/management approval within 30 days?'),
    ('qu_risk_mgmt_6', 'Is a centralized risk register maintained/updated quarterly (all open risks listed, owners assigned, RPN tracked), accessible via QMS dashboard?'),
    ('qu_risk_mgmt_7', 'Are risk assessments periodically reviewed/updated (e.g., annually or post-change) with documented rationale for unchanged risks and mitigation progress tracked?'),
    ('qu_risk_mgmt_8', 'Is QA responsible for QRM oversight (facilitator qualification, template approval, final sign-off), ensuring science/risk-based decisions per governance SOP?'),
    ('qu_risk_mgmt_9', 'Are risk assessments linked to change control (pre/post RPN), deviations (triggered >RPN 50), and CAPA (mitigation actions) with bidirectional cross-references?'),
    ('qu_risk_mgmt_10', 'Are all major decisions (batch release, validation scope, supplier qualification) documented with science/risk justification referencing specific risk assessment output?'),
    ('qu_risk_mgmt_11', 'Are risk acceptance criteria clearly documented (e.g., RPN<50 acceptable, 50-100 monitor, >100 mitigate) and consistently applied across assessments?'),
    ('qu_risk_mgmt_12', 'Are high-impact risk assessments (sterile, potent API) conducted by cross-functional teams (min 4 departments: QA, production, QC, engineering) with attendance/roles documented?'),
    ('qu_risk_mgmt_13', 'Are risk mitigation plans tracked (owners, due dates, % complete) via QMS dashboard, with monthly reviews and escalations for delays >30 days?'),
    ('qu_risk_mgmt_14', 'Is residual risk post-mitigation quantitatively re-evaluated (new RPN calculated) and documented, confirming reduction to acceptable levels?'),
    ('qu_risk_mgmt_15', 'Are risks communicated to relevant personnel via training, dashboards, or SOPs, with acknowledgment/read receipts for critical risks (RPN>100)?'),
    ('qu_risk_mgmt_16', 'Are risks to data integrity (ALCOA+ violations, system access, manual transcription) explicitly included in QRM assessments for computerized systems and manual processes?'),
    ('qu_risk_mgmt_17', 'Is risk scoring reproducible across teams (inter-rater reliability ≥85% agreement on RPN for test assessments) with calibration training/refresher annually?'),
    ('qu_risk_mgmt_18', 'Are special risks (sterility assurance >10⁻⁶ SAL, cross-contamination MAC/PDE, supply chain dual sourcing) addressed explicitly with dedicated FMEA/HACCP?'),
    ('qu_risk_mgmt_19', 'Are QRM templates standardized across departments (FMEA sheet format, scoring scales, output report) and approved/controlled by QA?'),
    ('qu_risk_mgmt_20', 'Are batch disposition decisions (release/hold) documented with risk assessment reference (e.g., deviation RPN reduced to 40 post-CAPA)?'),
    ('qu_risk_mgmt_21', 'Are completed risk assessments verified by QA for methodology compliance, scoring consistency, and mitigation feasibility before formal closure?'),
    ('qu_risk_mgmt_22', 'Are QRM outcomes (top risks, mitigation status) reviewed during Quality Management Review (QMR) meetings quarterly, with actions assigned?'),
    ('qu_risk_mgmt_23', 'Are new processes/equipment validated using QRM principles (prospective FMEA before IQ/OQ/PQ, critical controls defined)?'),
    ('qu_risk_mgmt_24', 'Are low-risk designations (RPN<20) justified with evidence (historical data, control strength, low severity) rather than assumption?'),
    ('qu_risk_mgmt_25', 'Are QRM failures (mitigation ineffective, risks materialized) trended quarterly for systemic QRM program improvement (training, tools, oversight)?'),
]

for i, (q_id, q_text) in enumerate(far_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_field_alerts", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Field Alert Reports questions")

for i, (q_id, q_text) in enumerate(change_control_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_change_control", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Change Control questions")

for i, (q_id, q_text) in enumerate(risk_mgmt_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_risk_mgmt", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Quality Risk Management questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating FAR, Change Control, and Quality Risk Management")
