#!/usr/bin/env python3
"""
Update CSV, Tech Transfer, APQR, Disposition, Supplier Quality, Management Review
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Computer System Validation (1.13)
csv_questions = [
    ('qu_csv_1', 'Is there an approved CSV SOP aligned with GAMP 5 (risk-based lifecycle), FDA 21 CFR Part 11 (records/signatures), and Annex 11 (CSV principles), covering all GxP systems?'),
    ('qu_csv_2', 'Are computerized systems categorized by GxP impact (Category 1 configurable, 3 non-configured, 4/5 custom/infrastructure) per GAMP 5 with documented rationale?'),
    ('qu_csv_3', 'Are User Requirements Specifications (URS) approved by stakeholders before system design/procurement, with traceability matrix to functional specs?'),
    ('qu_csv_4', 'Are Functional (FS) and Design Specifications (DS) documented, traceable to URS via matrix, and approved before configuration/development?'),
    ('qu_csv_5', 'Are GAMP 5 risk assessments (FMEA for data integrity, patient safety) performed for each system, classifying modules/tests pre-validation?'),
    ('qu_csv_6', 'Are Validation Master Plans (VMP) or system Validation Plans developed with clear acceptance criteria, OQ/PQ scope based on risk?'),
    ('qu_csv_7', 'Are IQ (installation), OQ (operational), PQ (performance) protocols approved/executed with traceability to URS/FS, deviations closed ≤30 days?'),
    ('qu_csv_8', 'Are electronic records/signatures Part 11 compliant (unique ID, audit trail, non-repudiation, validation of intent)?'),
    ('qu_csv_9', 'Are secure audit trails enabled (user actions, timestamps, before/after values) and reviewed periodically (weekly critical, monthly routine) with exceptions investigated?'),
    ('qu_csv_10', 'Are system updates/patches managed via change control (risk assessment, regression testing, re-qualification per GAMP)?'),
    ('qu_csv_11', 'Are software suppliers audited/qualified (questionnaire, audit report, right-to-audit clause) before use for GxP systems?'),
    ('qu_csv_12', 'Are data backup/restore procedures validated (full/incremental schedules, media integrity, recovery time ≤4h) with quarterly tests ≥99% success?'),
    ('qu_csv_13', 'Are disaster recovery plans (BCP/DR) defined/tested annually (failover ≤24h, full recovery ≤72h) for critical GxP systems?'),
    ('qu_csv_14', 'Are GxP spreadsheets validated (change control, password protection, formula lock, peer review) or prohibited with alternatives enforced?'),
    ('qu_csv_15', 'Are system access rights role-based, reviewed semi-annually (certification by managers), with immediate revocation for role changes?'),
    ('qu_csv_16', 'Are training records maintained for system users (initial 4h + annual refresher, competency ≥90%) with matrix current ≥95%?'),
    ('qu_csv_17', 'Are system performance (uptime ≥99%) and error logs reviewed monthly, with trends triggering maintenance/CAPA?'),
    ('qu_csv_18', 'Are interfaces between GxP systems (LIMS-MES, ERP-QMS) validated for data integrity (accuracy, completeness, no loss)?'),
    ('qu_csv_19', 'Are data archival processes validated (WORM compliance, readability 10yrs, migration tested) per retention schedules?'),
    ('qu_csv_20', 'Are cyber-security controls (firewall, encryption, vulnerability scans) validated/tested annually for GxP systems?'),
    ('qu_csv_21', 'Are periodic reviews performed (annual for critical, 2yr routine) assessing compliance, performance, and retirement needs?'),
    ('qu_csv_22', 'Are system decommissioning procedures defined (data migration, archival, secure wipe) with final QA sign-off?'),
    ('qu_csv_23', 'Are legacy systems risk-evaluated for Part 11/Annex 11 gaps, with remediation/migration plans if non-compliant?'),
    ('qu_csv_24', 'Are vendor-supplied documentation (IQ/OQ scripts) and patches independently verified (hands-on testing) before acceptance?'),
    ('qu_csv_25', 'Are validation documents archived securely (fireproof/digital WORM, retrievable <30min) for inspection (lifecycle +1yr)?'),
]

for i, (q_id, q_text) in enumerate(csv_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_csv", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated CSV questions")

# Technology Transfer (1.14)
tech_transfer_questions = [
    ('qu_tech_transfer_1', 'Is there an approved TT SOP covering development-to-commercial and site-to-site transfers, defining stages (knowledge transfer, scale-up, validation, PPQ oversight)?'),
    ('qu_tech_transfer_2', 'Are TT teams cross-functional (R&D/formulation, Manufacturing, QA, QC, Engineering/RA) with defined roles, ≥5 members, and kickoff charter signed?'),
    ('qu_tech_transfer_3', 'Is process knowledge (CPPs/CQAs, design space, historical data) and critical parameters transferred in documented format (Tech Transfer Package/Dossier)?'),
    ('qu_tech_transfer_4', 'Are scale-up risks (heat/mass transfer, mixing) evaluated/documented via FMEA (RPN prioritized) pre-pilot batches?'),
    ('qu_tech_transfer_5', 'Are prospective risk assessments (FMEA/PHA) performed at TT milestones (lab→pilot→commercial) with residual risk tracked?'),
    ('qu_tech_transfer_6', 'Are analytical methods validated (sending site ICH Q2) or verified (receiving equivalence ≥95% accuracy) before TT completion?'),
    ('qu_tech_transfer_7', 'Are equipment comparability studies (ribbon blender vs high shear, ribbon vs V-blender torque) performed between sending/receiving sites?'),
    ('qu_tech_transfer_8', 'Are cleaning requirements (worst-case, MAC/PDE) assessed during TT, with bracketing/matrixing justified for receiving site?'),
    ('qu_tech_transfer_9', 'Are cleaning validation limits (swab/rinse, visuals) reassessed for TT scale/equipment changes pre-commercial?'),
    ('qu_tech_transfer_10', 'Are Hold Time studies (dirty/clean equipment, in-process intermediates) transferred and verified at receiving site (challenge max hold)?'),
    ('qu_tech_transfer_11', 'Are ≥3 engineering batches planned/executed at target scale with QA oversight (real-time review, deviation control)?'),
    ('qu_tech_transfer_12', 'Are process control strategies (PAT, in-process limits, alarms) established/verified during TT engineering runs?'),
    ('qu_tech_transfer_13', 'Are Technology Transfer Reports (summary data, gaps closed, readiness) reviewed/approved by QA before PPQ?'),
    ('qu_tech_transfer_14', 'Are PPQ batches (≥3 consecutive) planned with TT oversight (sending site support, enhanced sampling)?'),
    ('qu_tech_transfer_15', 'Are material attribute differences (API PSD, excipient grade, supplier) assessed with comparability protocols pre-TT?'),
    ('qu_tech_transfer_16', 'Are in-process/product sampling plans reassessed during TT for scale (stat power ≥90%)?'),
    ('qu_tech_transfer_17', 'Are deviations during TT trended (by phase, root cause) for learnings incorporated into commercial process?'),
    ('qu_tech_transfer_18', 'Are training requirements defined for receiving operators (SOPs, OJT, ≥40h) with qualification ≥90% competency pre-PPQ?'),
    ('qu_tech_transfer_19', 'Are scale-up deviations (yield <90%, OOS) escalated to TT team/QA for immediate disposition?'),
    ('qu_tech_transfer_20', 'Are TT documents (protocols, data, reports) controlled (unique IDs, versions) and archived ≥ product lifecycle +1yr?'),
    ('qu_tech_transfer_21', 'Are equipment gaps (no high shear mixer) identified/mitigated (protocol equivalence, rental validation) before TT?'),
    ('qu_tech_transfer_22', 'Are utilities (HVAC, WFI, compressed air) assessed for suitability (qualify, microbial specs) at receiving site pre-TT?'),
    ('qu_tech_transfer_23', 'Are initial 3 commercial batches monitored more frequently (daily IPC, enhanced QC release) post-TT?'),
    ('qu_tech_transfer_24', 'Is process robustness data (CPV stage 1, historical) reviewed during TT to set commercial controls?'),
    ('qu_tech_transfer_25', 'Are TT activities (scale factors, minor tweaks) linked to change controls with regulatory assessment?'),
]

for i, (q_id, q_text) in enumerate(tech_transfer_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_tech_transfer", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Technology Transfer questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating CSV and Tech Transfer")
