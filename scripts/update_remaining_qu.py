#!/usr/bin/env python3
"""
Bulk update remaining Quality Unit questions (Document Management, Complaint Management,
Data Integrity, Training Management, Field Alert Reports, Change Control, Quality Risk Management)
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Document Management (1.4) - 25 questions (note: original has 26, need to fix)
document_mgmt_questions = [
    ('qu_document_mgmt_1', 'Does an approved SOP comprehensively cover document creation (template approval), revision (change control workflow), approval (multi-level sign-off), issuance (controlled copies), and archiving (retention schedule) for all GxP records?'),
    ('qu_document_mgmt_2', 'Are document templates (SOPs, batch records, forms) standardized across the site using a master template library maintained by QA, with mandatory fields, headers/footers, and revision blocks?'),
    ('qu_document_mgmt_3', 'Are revisions controlled via unique version numbers (e.g., SOP-001 Rev 5.1), with complete change history tables documenting what changed, rationale, and approver signatures for every update?'),
    ('qu_document_mgmt_4', 'Are obsolete documents removed from point-of-use locations (workstations, shop floor binders) within 24h of supersession, verified by post-withdrawal audits, with physical destruction witnessed?'),
    ('qu_document_mgmt_5', 'Are electronic documents protected from unauthorized modification via role-based access controls, electronic signatures (21 CFR Part 11/Annex 11 compliant), and audit trails recording all views/edits?'),
    ('qu_document_mgmt_6', 'Do all controlled document copies (paper/electronic) bear unique identifiers (e.g., DOC-SOP-001-Rev5-CopyA001) or visible version control numbers, with uncontrolled status watermarked where applicable?'),
    ('qu_document_mgmt_7', 'Are metadata fields (author, reviewer(s), approver(s), effective date, next review date) 100% complete, accurate, and uneditable post-approval for all controlled documents?'),
    ('qu_document_mgmt_8', 'Are periodic reviews defined (e.g., SOPs every 3 years) and executed on schedule, with evidence of review (change/no change decision, signature) in the document record?'),
    ('qu_document_mgmt_9', 'Are operational forms (deviation, OOS, cleaning checklists) controlled, uniquely numbered, and traceable to master document templates, with local copies prohibited?'),
    ('qu_document_mgmt_10', 'Are logbooks (equipment, batch, cleaning) pre-numbered, bound (non-erasable), issued by QA with unique IDs, and reconciled for completeness upon return?'),
    ('qu_document_mgmt_11', 'Are handwritten corrections performed per GDP (single black line strikeout, legible original visible, immediate justification, maker/verifier initials + date, no white-out/overwriting)?'),
    ('qu_document_mgmt_12', 'Upon document revision, are affected personnel retrained (documented attendance, competency assessment ≥90%) with training matrix updated before effective date?'),
    ('qu_document_mgmt_13', 'Are interim SOPs, work instructions, or memos formally controlled (unique number, limited duration ≤90 days, QA approval, distribution list, destruction post-supersession)?'),
    ('qu_document_mgmt_14', 'Is the archive room environmentally controlled (15-25°C, 40-60%RH, fireproof, secure access) with continuous monitoring, quarterly mapping, and alarm response verified?'),
    ('qu_document_mgmt_15', 'Are retention timelines defined per regulation (e.g., batch records 1yr post-expiry, complaints 5yrs, stability 10yrs) and consistently applied with automated purge alerts?'),
    ('qu_document_mgmt_16', 'Are electronic document management systems (eQMS, LIMS) validated per GAMP 5/Annex 11 (URS, IQ/OQ/PQ, CSV), with change control and periodic review executed?'),
    ('qu_document_mgmt_17', 'Are executed batch records protected from damage/fading/loss via fireproof storage, microfilming/digital archiving (legible 10yrs), and duplicate backups tested annually?'),
    ('qu_document_mgmt_18', 'Are controlled forms used consistently 100%, with local photocopies prohibited and detected via audits (zero tolerance, training reinforcement)?'),
    ('qu_document_mgmt_19', 'Are critical documents (SOPs, MPRs) available at point-of-use (QR codes, tablets, laminated binders) with version verification before operations start?'),
    ('qu_document_mgmt_20', 'Are deviations from documentation procedures (e.g., missing signatures, wrong version) captured as formal deviations with investigation and CAPA?'),
    ('qu_document_mgmt_21', 'Are multilingual documents (local language versions) verified for consistency via back-translation or bilingual review, with identical critical content/limits as English master?'),
    ('qu_document_mgmt_22', 'Are scanned documents (legacy paper records) legible (300dpi min, no shadows), complete (full pages, signatures visible), and validated for OCR accuracy ≥99% where searchable?'),
    ('qu_document_mgmt_23', 'Are external standards/guidelines/compendia (USP, PhEur, ICH) tracked for updates via subscription alerts, with impact assessments and internal document revisions completed ≤60 days?'),
    ('qu_document_mgmt_24', 'Are secure backups maintained for electronic documents (daily incremental, weekly full, offsite/cloud, encrypted), with quarterly restore tests confirming 100% recovery <4h?'),
    ('qu_document_mgmt_25', 'Are document destruction activities (obsolete SOPs, expired retains) logged with QA approval, witnessed execution (shredding/weighing), and certificates retained per SOP?'),
]

# Replace Document Management questions
for i, (q_id, q_text) in enumerate(document_mgmt_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_document_mgmt", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Document Management questions")

# Complaint Management (1.5)
complaint_mgmt_questions = [
    ('qu_complaint_mgmt_1', 'Is there an approved SOP defining complaint receipt (phone/email/log), logging, triage (critical/major/minor per criteria), investigation (scope/timeline), and closure (response/CAPA) for all product/service complaints?'),
    ('qu_complaint_mgmt_2', 'Are complaints categorized as critical (patient harm potential), major (quality defect, no harm), or minor (cosmetic/labeling) with clear, documented definitions, examples, and initial triage within 24h?'),
    ('qu_complaint_mgmt_3', 'Are complaint investigations initiated within defined timelines (critical ≤24h, major ≤3 days, minor ≤7 days) with aging reports tracked weekly and escalations for delays?'),
    ('qu_complaint_mgmt_4', 'Are complaints indicating adverse drug events (ADEs) or safety signals escalated to pharmacovigilance/global safety without delay (≤24h) with PV case# cross-referenced in complaint file?'),
    ('qu_complaint_mgmt_5', 'Is market history/trending (prior complaints, batch performance, OOS rates for same lot/supplier) reviewed during every complaint assessment and documented in investigation scope?'),
    ('qu_complaint_mgmt_6', 'Are reserve/retain samples (same lot, same packaging) evaluated (visual/microbial/analytical) as part of every quality complaint investigation, with side-by-side comparison to complainant sample?'),
    ('qu_complaint_mgmt_7', 'Are manufacturing (BMR/BPR), laboratory (OOS history), and QC records reviewed for every market complaint, with specific cross-references to batch documentation?'),
    ('qu_complaint_mgmt_8', 'Are complaint investigation conclusions scientifically justified (data, testing, stats) and free from unsubstantiated assumptions (e.g., customer error without evidence)?'),
    ('qu_complaint_mgmt_9', 'Is distribution data (ship dates, quantities, customers, storage/transit conditions) reviewed to identify and quarantine potentially affected lots within 48h?'),
    ('qu_complaint_mgmt_10', 'Are recurring complaints (≥3 same type/product in 6 months) automatically escalated to systemic investigations with FMEA or root cause trending beyond single lot?'),
    ('qu_complaint_mgmt_11', 'Are packaging integrity complaints (leaks, broken seals) analyzed with statistical sampling (AQL 1.0%) of retains and trend analysis by supplier/lot?'),
    ('qu_complaint_mgmt_12', 'Are complaint files complete with complainant communication logs, evidence (photos, test data), investigation report, and CAPA status, retained ≥1yr post-expiry?'),
    ('qu_complaint_mgmt_13', 'Are complaint conclusions supported by laboratory re-testing data (complainant + retain samples) when chemical/microbial analysis is indicated, with full raw data attached?'),
    ('qu_complaint_mgmt_14', 'Are products returned with complaints quarantined immediately upon receipt, visually/microbiologically examined, and dispositioned by QA within 7 days?'),
    ('qu_complaint_mgmt_15', 'Are complaint files in electronic systems protected with audit trails (21 CFR Part 11/Annex 11) recording all entries, edits, and approvals with no deletions?'),
    ('qu_complaint_mgmt_16', 'Are closed complaint investigations reopened when new information emerges (e.g., related recall, pattern identification) within SOP criteria (≤1yr post-closure)?'),
    ('qu_complaint_mgmt_17', 'Are counterfeit or tampering complaints escalated to regulatory agencies (FDA MedWatch, EMA) and law enforcement within 24h, with samples preserved for forensic analysis?'),
    ('qu_complaint_mgmt_18', 'Are distribution-related complaints (temp excursions, damage) evaluated via carrier data loggers, route mapping, and cold chain validation records?'),
    ('qu_complaint_mgmt_19', 'Are all customer communications (acknowledgment ≤48h, final response ≤30 days) documented, pre-approved by QA, and retained verbatim in complaint file?'),
    ('qu_complaint_mgmt_20', 'Are timelines for complaint closure risk-based (critical ≤15 days, major ≤30 days, minor ≤60 days) and monitored via aging dashboard with escalations?'),
    ('qu_complaint_mgmt_21', 'Is complaint data (volume, types, trends, closure rates) integrated into Annual Product Quality Review (APQR) with root cause analysis and CAPA effectiveness?'),
    ('qu_complaint_mgmt_22', 'Is annual training provided to all complaint handlers (receipt, triage, investigation) with competency assessment (≥90% test score) and refresher on SOP changes?'),
    ('qu_complaint_mgmt_23', 'Are complaint investigations traced to CAPA, change control, or risk assessments when root causes indicate systemic issues, with cross-references in all records?'),
    ('qu_complaint_mgmt_24', 'Are complaint trending reports (monthly by product/type/source, Pareto top 5) reviewed by senior management (Quality Committee) with action items assigned?'),
    ('qu_complaint_mgmt_25', 'Are complaint investigations extended to sister plants/manufacturing sites using same material/process, with shared findings and coordinated CAPA?'),
]

for i, (q_id, q_text) in enumerate(complaint_mgmt_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_complaint_mgmt", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Complaint Management questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating Document Management and Complaint Management")
