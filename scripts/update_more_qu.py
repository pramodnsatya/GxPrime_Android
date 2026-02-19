#!/usr/bin/env python3
"""
Update Returned Drugs, Audit Management, CSV, Tech Transfer, APQR, Disposition, Supplier, Management Review
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Returned and Salvaged Drug Products (1.11)
returned_drugs_questions = [
    ('qu_returned_drugs_1', 'Is there an approved SOP covering receipt (immediate quarantine), evaluation (visual/analytical), disposition (destroy/reprocess/release), and documentation for all returned products?'),
    ('qu_returned_drugs_2', 'Are returned goods logged within 24h with batch#/lot#, quantity returned, customer/distributor details, reason (damage/OOS/complaint), and receipt date?'),
    ('qu_returned_drugs_3', 'Are returns quarantined (physically segregated ≥2m, labeled RETURNED QUARANTINE lot#XYZ, restricted access) to prevent mix-ups with releasable stock upon receipt?'),
    ('qu_returned_drugs_4', 'Are visual inspections (container integrity, labeling, tampering signs) performed/documented on 100% of returned containers by trained QA/warehouse personnel within 48h?'),
    ('qu_returned_drugs_5', 'Are storage/transportation conditions reviewed (temp logs, chain of custody, shipping damage) for return justification, with excursion impact assessed?'),
    ('qu_returned_drugs_6', 'Are returned goods assessed for tampering/counterfeiting (seal breach, holograms, serialization check) with escalation if suspected?'),
    ('qu_returned_drugs_7', 'Are full quality investigations (per 211.192 if associated batches implicated) performed for each return reason (e.g., potency failure, contamination), with root cause?'),
    ('qu_returned_drugs_8', 'Are microbiological risks (beyond-use stability, sterility breach potential) evaluated for sterile product returns via targeted testing/quarantine extension?'),
    ('qu_returned_drugs_9', 'Are chemical/analytical assessments (assay, impurities, dissolution) performed on retains/returns when spec doubt exists, with compendial methods?'),
    ('qu_returned_drugs_10', 'Are salvage operations prevented unless justified (lab data proving specs met) and approved by QA Head, with no disaster-exposed product salvaged?'),
    ('qu_returned_drugs_11', 'Are salvage operations (rarely approved) documented with scientific rationale (test data, stability justification), QA approval, and full traceability?'),
    ('qu_returned_drugs_12', 'Are wholesalers/distributors audited annually (GDP compliance, storage 15-25°C, FIFO) to prevent improper storage contributing to returns?'),
    ('qu_returned_drugs_13', 'Are decision outcomes (reprocess/rerelabel/destroy) documented with QA sign-off, rationale, and batch disposition within 30 days of receipt?'),
    ('qu_returned_drugs_14', 'Are returned goods used for complaint investigation (side-by-side retain comparison) where applicable, with chain-of-custody maintained?'),
    ('qu_returned_drugs_15', 'Is destruction of rejected returns documented (weighed, incinerated/rendered, witnessed by QA/production) with certificates retained ≥1yr post-expiry?'),
    ('qu_returned_drugs_16', 'Are returned goods data (volume, reasons, disposition) included in APQR trend analysis with root cause/Pareto for each product?'),
    ('qu_returned_drugs_17', 'Are falsified/counterfeit returns escalated to regulatory authorities (FDA/EMA) within 24h with sample preservation for forensic analysis?'),
    ('qu_returned_drugs_18', 'Are retesting/reprocessing decisions scientifically justified (stability data, risk assessment) with full validation before rerelabeling/release?'),
    ('qu_returned_drugs_19', 'Are returned product samples retained (same conditions as product, 1yr post-expiry) for potential investigation/FAR?'),
    ('qu_returned_drugs_20', 'Are returns evaluated for Field Alert Report (FAR) obligations per 21 CFR 314.81(b)(1) if distributed batches implicated?'),
    ('qu_returned_drugs_21', 'Is segregation between returned and market stock ensured (separate racks/ERP status QUARANTINE RETURNED) with zero mix-ups verified monthly?'),
    ('qu_returned_drugs_22', 'Are returned temperature-sensitive products evaluated using real-time stability data/excursion justification before any disposition?'),
    ('qu_returned_drugs_23', 'Are discrepancies between returned quantities and distribution records (>5% variance) investigated as potential diversion/theft?'),
    ('qu_returned_drugs_24', 'Are records of returns retained per GMP (name/potency, lot#, reason, qty, disposition date, ultimate fate) for ≥1yr post-expiry?'),
    ('qu_returned_drugs_25', 'Are return trends (quarterly by product/customer/reason) reviewed during management review with preventive actions assigned?'),
]

for i, (q_id, q_text) in enumerate(returned_drugs_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_returned_drugs", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Returned Drugs questions")

# Audit Management (1.12)
audit_questions = [
    ('qu_audit_1', 'Is there an approved annual audit program covering internal (self-inspections), external (contractors), and supplier audits, with ≥90% execution rate and risk-based prioritization?'),
    ('qu_audit_2', 'Are internal/external auditors trained (40h initial +8h annual refresher) and qualified based on experience (≥3yr GMP), competency assessments (≥90% mock audit), and independence?'),
    ('qu_audit_3', 'Are annual internal audit schedules risk-based (FMEA RPN>50, prior findings, new processes) with documented rationale and approved by Quality Head?'),
    ('qu_audit_4', 'Are audit checklists developed/updated using regulatory guidelines (FDA Forms 483, Eudralex Vol 4, ICH Q10) covering all GMP areas with version control?'),
    ('qu_audit_5', 'Are audit observations categorized (critical/major/minor) using objective criteria (patient risk, GMP violation type) with definitions/examples in SOP?'),
    ('qu_audit_6', 'Are draft audit reports issued to auditees within defined timelines (critical ≤7 days, routine ≤14 days) with final QA sign-off ≤30 days?'),
    ('qu_audit_7', 'Are auditees required to respond (root cause, CAPA plan, timelines) within defined timeframe (critical ≤14 days, major ≤30 days, minor ≤45 days)?'),
    ('qu_audit_8', 'Are audit responses evaluated for completeness/adequacy by independent QA reviewer using standardized checklist before acceptance?'),
    ('qu_audit_9', 'Are CAPAs from audits created/tracked to closure (evidence, effectiveness check) via QMS, with ≤5% overdue at any time?'),
    ('qu_audit_10', 'Are repeat/recurring findings trended quarterly (by area, product, supplier) with systemic CAPA escalation?'),
    ('qu_audit_11', 'Are high-risk/critical findings escalated to senior management (Quality Committee) within 48h with interim controls required?'),
    ('qu_audit_12', 'Are follow-up audits scheduled/executed for critical findings (≤6 months post-CAPA due) to verify effectiveness?'),
    ('qu_audit_13', 'Are suppliers audited based on risk classification (critical API annual, low-risk 3yr) with qualification status matrix updated post-audit?'),
    ('qu_audit_14', 'Are audit trails/system logs reviewed during Computerized System Validation (CSV) audits per Part 11/Annex 11 requirements?'),
    ('qu_audit_15', 'Are audit reports controlled (unique IDs, versions) and archived securely (fireproof, 5yr retention) with retrievability <30min?'),
    ('qu_audit_16', 'Are external audit findings (FDA 483s, MHRA GLP, customer audits) integrated into internal audit planning/next schedule?'),
    ('qu_audit_17', 'Are corporate audits harmonized with site audits (shared checklists, findings consolidated) to avoid duplication?'),
    ('qu_audit_18', 'Are third-party auditors pre-qualified (credentials, references, site audit) before use, with performance monitored via post-audit reviews?'),
    ('qu_audit_19', 'Are auditors independent of audited departments (no line reporting <2 levels), with conflicts declared/resolved pre-assignment?'),
    ('qu_audit_20', 'Are mock regulatory inspections performed ≥2x/year (full site, unannounced) with findings trended and preparedness score ≥90%?'),
    ('qu_audit_21', 'Are audit KPIs tracked monthly (schedule adherence ≥95%, CAPA closure ≤90 days, recurrence <5%) via dashboard?'),
    ('qu_audit_22', 'Are audit summaries (key findings, CAPA status, trends) included in quarterly management reviews with actions assigned?'),
    ('qu_audit_23', 'Are unannounced audits included in program for suppliers/high-risk areas (≥20% supplier audits unannounced)?'),
    ('qu_audit_24', 'Are audit tools (checklists, templates, scoring matrices) version controlled, approved by QA, and training provided on updates?'),
    ('qu_audit_25', 'Are auditees trained annually on audit preparedness (document readiness, response SOPs, mock drills) with ≥90% participation?'),
]

for i, (q_id, q_text) in enumerate(audit_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_audit", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Audit Management questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating Returned Drugs and Audit Management")
