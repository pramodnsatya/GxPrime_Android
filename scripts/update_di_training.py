#!/usr/bin/env python3
"""
Update Data Integrity, Training Management, Field Alert Reports, Change Control, Quality Risk Management
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Data Integrity (1.7)
data_integrity_questions = [
    ('qu_data_integrity_1', 'Is there a site-wide data integrity policy explicitly aligned with ALCOA+ principles (Attributable, Legible, Contemporaneous, Original, Accurate + Complete, Consistent, Enduring, Available), approved by senior management, and communicated?'),
    ('qu_data_integrity_2', 'Are roles/responsibilities for DI governance clearly defined (e.g., Data Stewards, System Owners, QA Reviewers) in the DI policy/SOP, with RACI matrix and annual training acknowledgment?'),
    ('qu_data_integrity_3', 'Are all GxP electronic systems (LIMS, QMS, ERP, MES) validated per GAMP 5/Annex 11 (URS, IQ/OQ/PQ/CSV) to ensure secure data handling, with current validation status documented?'),
    ('qu_data_integrity_4', 'Are secure, time-stamped audit trails enabled/reviewed/maintained for all GxP systems (raw data/metadata changes), with defined review frequency (e.g., weekly critical, monthly routine)?'),
    ('qu_data_integrity_5', 'Are data access controls role-based (least privilege principle), with periodic reviews (semi-annual access certification by managers) and immediate revocation for leavers?'),
    ('qu_data_integrity_6', 'Are unique user IDs/passwords (no shared accounts) assigned to each authorized GxP user, with password policies (8+ chars, 90-day expiry, no reuse of last 10) enforced?'),
    ('qu_data_integrity_7', 'Are raw data, metadata (audit trails, configs), and contextual information retained/protected (immutable format, lifecycle defined) for regulatory periods (e.g., 1yr post-expiry)?'),
    ('qu_data_integrity_8', 'Are hybrid paper-electronic systems minimized (<10% GxP records), and if used, properly controlled with certified true copies, reconciliation, and retention per DI SOP?'),
    ('qu_data_integrity_9', 'Are paper records contemporaneous (real-time entry), legible (permanent ink), and compliant with GDP (single-line corrections, no erasable ink/whiteout, dual initials)?'),
    ('qu_data_integrity_10', 'Are system clocks synchronized (NTP server, daily check ±1min accuracy) and protected from manual changes, with drift logs reviewed monthly?'),
    ('qu_data_integrity_11', 'Are unauthorized data edits (audit trail flags) automatically tracked/investigated within 24h, with root cause and CAPA per DI incident SOP?'),
    ('qu_data_integrity_12', 'Are electronic signatures compliant with 21 CFR Part 11 (unique to signer, operationally equivalent to wet ink, intent disclosed), with non-repudiation verified?'),
    ('qu_data_integrity_13', 'Are data backups secure (encrypted, offsite), validated (frequency, format), and tested quarterly for restorability (<4h full recovery, 100% data integrity check)?'),
    ('qu_data_integrity_14', 'Are data integrity risks (e.g., manual transcription, spreadsheet errors, access abuse) explicitly included in Quality Risk Management (FMEA RPN>50) with mitigations?'),
    ('qu_data_integrity_15', 'Are all GxP personnel trained in DI principles (ALCOA+ awareness 2h annual, role-specific 4h initial), with competency assessment ≥90% pass rate?'),
    ('qu_data_integrity_16', 'Is local data storage on desktops/USB/personal devices prohibited for GxP data, enforced via Group Policy/DLP, with violations tracked as DI deviations?'),
    ('qu_data_integrity_17', 'Are spreadsheet calculations used in GxP decisions validated (change-controlled, password-protected, formula integrity checks) or migrated to validated systems?'),
    ('qu_data_integrity_18', 'Are audit trail reviews performed at defined risk-based intervals (daily critical systems, weekly routine, monthly full), with exceptions investigated ≤24h?'),
    ('qu_data_integrity_19', 'Are data integrity incidents (deletion, falsification, access abuse) escalated to senior management (Quality Head) within 24h, with root cause and CAPA required?'),
    ('qu_data_integrity_20', 'Is governance defined for cloud-based GxP systems (e.g., SLAs, data sovereignty, subcontracting approval) with annual third-party audits ≥90% compliance?'),
    ('qu_data_integrity_21', 'Are DI breaches, audit trail exceptions, and training compliance included in annual management reviews with KPIs (e.g., DI incidents <1%, review coverage 100%)?'),
    ('qu_data_integrity_22', 'Are temporary/shared logins prohibited, with monitoring for violations (e.g., concurrent sessions) and automatic lockout after 3 failed attempts?'),
    ('qu_data_integrity_23', 'Are electronic record retention rules followed (e.g., migrate to WORM archival post-active use, readability verified 10yrs) per regulatory timelines?'),
    ('qu_data_integrity_24', r'Are atypical data trends (e.g., clustered OOS, uniform values) investigated as potential DI issues using statistical tests (Benford\'s Law, control charts)?'),
    ('qu_data_integrity_25', 'Are DI controls periodically assessed through targeted internal audits/self-inspections (annual coverage ≥90% systems), with findings trended and CAPA tracked?'),
]

for i, (q_id, q_text) in enumerate(data_integrity_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_data_integrity", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Data Integrity questions")

# Training Management (1.8)
training_mgmt_questions = [
    ('qu_training_1', 'Is there an approved training SOP defining competency (knowledge+skills), qualification (On-Job Training/assessment), retraining triggers (SOP change, incident, 2yrs), and documentation standards?'),
    ('qu_training_2', 'Are department-specific training matrices maintained (current, electronic/paper, ≥95% completion rate), updated quarterly, and signed by managers/QA with gap alerts?'),
    ('qu_training_3', 'Are training records complete (attendee, trainer, date, content/version, assessment score), contemporaneous (signed same day), and audit-ready (retrievable <30min)?'),
    ('qu_training_4', 'Are new employees completing Phase I/II GMP orientation + job-specific training (≥40h) and qualified before independent GxP activities, per probation checklist?'),
    ('qu_training_5', 'Are critical GxP operations (aseptic filling, weighing, deviations) restricted to qualified personnel only, verified via badge/swipe access or supervisor log?'),
    ('qu_training_6', 'Are periodic refresher trainings scheduled/tracked (annual GMP awareness, 2yr job-specific) with automated LMS reminders and ≤5% overdue rate?'),
    ('qu_training_7', 'Are effectiveness checks performed for critical/CAPA-linked training (post-quiz ≥90%, observation audit, KPI improvement) within 90 days of delivery?'),
    ('qu_training_8', 'Upon SOP revision, are affected personnel retrained (read+acknowledge ≤30 days effective date) with completion ≥95% before using new version?'),
    ('qu_training_9', 'Are on-the-job training (OJT) sessions documented with qualified trainer signatures, trainee competency checklist (≥90% steps demonstrated), and supervisor verification?'),
    ('qu_training_10', 'Are training courses reviewed annually for relevance/accuracy (SME sign-off, post-training feedback ≥80% satisfaction) with obsolete content updated?'),
    ('qu_training_11', 'Are e-learning modules validated (content accuracy, LMS tracking completion/tests, no unauthorized skips) with periodic review (annual quiz refresh)?'),
    ('qu_training_12', 'Are training gaps identified in OOS/deviations/CAPA addressed via targeted retraining with effectiveness verification before closure?'),
    ('qu_training_13', 'Are temporary staff/contractors trained equivalently to full-time (matrix inclusion, qualification before GxP work) with access revoked post-contract?'),
    ('qu_training_14', 'Are consultants/contractors included in training program (site induction, job SOPs, qualification) with records retained ≥ contract duration +1yr?'),
    ('qu_training_15', 'Are training facilities adequate (quiet, GMP visuals, audio-visual equipment functional) and free from distractions, verified by annual facility audit?'),
    ('qu_training_16', 'Are training records protected from manipulation (electronic Part 11 audit trails, paper locked storage) with access logs reviewed semi-annually?'),
    ('qu_training_17', 'Are personnel requalified after prolonged inactivity (>3 months critical operations, >6 months routine) via refresher+OJT before resuming GxP duties?'),
    ('qu_training_18', 'Are training effectiveness quizzes scientifically designed (≥20 validated questions, ≥80% pass, psychometrics reviewed annually) for critical roles?'),
    ('qu_training_19', 'Is training data (completion rates, scores) integrated into performance management (appraisals, promotions) with low performers (<80% compliance) coached?'),
    ('qu_training_20', 'Are continuous learning programs available (quarterly GMP refreshers, annual compliance trends webinars) with ≥4h/year participation tracked per employee?'),
    ('qu_training_21', 'Are training KPIs monitored monthly (overdue <5%, competency ≥90%, effectiveness ≥85%) via dashboard reviewed in Quality Management Review?'),
    ('qu_training_22', 'Are supervisors accountable for team training completion (sign matrix monthly, escalate gaps), with non-compliance linked to performance metrics?'),
    ('qu_training_23', 'Are job descriptions aligned with competency requirements (specific trainings listed), reviewed annually with HR/QA approval?'),
    ('qu_training_24', 'Are critical quality incidents (OOS clusters, deviations) used for targeted retraining (root cause training gaps) with effectiveness pre-closure verification?'),
    ('qu_training_25', 'Are training files (matrices, records, gaps) proactively included in regulatory audit preparation packages with mock audit readiness ≥95%?'),
]

for i, (q_id, q_text) in enumerate(training_mgmt_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_training", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Training Management questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating Data Integrity and Training Management")
