#!/usr/bin/env python3
"""
Update APQR, Disposition, Supplier Quality, Management Review
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Annual Product Quality Review (1.15)
apqr_questions = [
    ('qu_apqr_1', 'Is there an approved SOP defining APQR process (data collection, analysis, conclusions, actions) and timeline (due Q1 annually, approved ≤60 days)?'),
    ('qu_apqr_2', 'Are APQRs prepared annually for every commercial product/strength/pack size marketed in prior year, including low volume?'),
    ('qu_apqr_3', 'Are deviation and CAPA trends analyzed (Pareto top 5 causes, closure rates ≥95%, effectiveness verified) for the product review period?'),
    ('qu_apqr_4', 'Are stability trends (OOS/OOT, shelf-life confirmation, excursions) graphed and analyzed with impact on expiry?'),
    ('qu_apqr_5', 'Are yield trends reviewed (stage-wise graphs, expected vs actual, abnormalities investigated) for all batches?'),
    ('qu_apqr_6', 'Are complaint trends (volume/type/rate per million units) analyzed/risk-assessed with linkages to deviations/CAPA?'),
    ('qu_apqr_7', 'Are product-specific change controls reviewed (approved, implemented, effectiveness post-change data stable)?'),
    ('qu_apqr_8', 'Are OOS/OOT trends (invalidated rate <10%, lab/manufacturing root causes) included with investigation summaries?'),
    ('qu_apqr_9', 'Is process capability evaluated (Cpk ≥1.33 critical CQAs, control charts) using statistical tools for ≥12 months data?'),
    ('qu_apqr_10', 'Are environmental monitoring trends (viable/non-viable excursions, cleanroom classification) reviewed for product impact?'),
    ('qu_apqr_11', 'Are supplier quality issues (COA deviations, OOS raw materials) summarized with audit findings and qualification status?'),
    ('qu_apqr_12', 'Are market withdrawals, recalls, or FAR events evaluated with root causes and preventive measures?'),
    ('qu_apqr_13', 'Are cleaning validation trends (swab failures, MAC compliance) and cross-contamination risks assessed?'),
    ('qu_apqr_14', 'Are batch failures/rejects summarized (count, reasons, investigations closed) with trend vs prior years?'),
    ('qu_apqr_15', 'Are manufacturing process improvements recommended based on trends (e.g., yield optimization, control tightening)?'),
    ('qu_apqr_16', 'Are equipment/facility issues (breakdowns, calibration drifts) reviewed for product-specific impact?'),
    ('qu_apqr_17', 'Are regulatory commitments (FDA responses, variations) tracked to closure with status in APQR?'),
    ('qu_apqr_18', 'Are APQRs reviewed/approved by QA Head with signatory confirming data accuracy and conclusions?'),
    ('qu_apqr_19', 'Are APQR action items (enhancements, studies) tracked to closure via QMS with due dates ≤12 months?'),
    ('qu_apqr_20', 'Are APQRs proactively referenced during PAI/routine inspections with executive summary provided?'),
    ('qu_apqr_21', 'Are training gaps/completion rates identified in APQR with linkage to quality events?'),
    ('qu_apqr_22', 'Are APQRs presented/reviewed by senior management (QMR) with decisions documented?'),
    ('qu_apqr_23', 'Are control charts/trending graphs used (Shewhart, Cpk plots) for yields, assays, impurities where ≥12 data points?'),
    ('qu_apqr_24', 'Is data integrity verified for APQR source data (audit trails reviewed, raw data attached, ALCOA+ compliant)?'),
    ('qu_apqr_25', 'Are APQRs archived (digital/paper secure) with retention ≥ product discontinuation +1yr?'),
]

for i, (q_id, q_text) in enumerate(apqr_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_apqr", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated APQR questions")

# Product Disposition (1.16)
disposition_questions = [
    ('qu_disposition_1', 'Is there a clear SOP defining batch disposition steps (production review → QC review → QA final release/quarantine) with timelines (≤5 days post-completion)?'),
    ('qu_disposition_2', 'Are QA reviewers independent of production (no dual hats, separate reporting line to Quality Head) per organizational chart?'),
    ('qu_disposition_3', 'Are batch production records (BPR/MPR) reviewed 100% for completeness/GMP compliance (dates, weights, initials, yields ±5%) before release?'),
    ('qu_disposition_4', 'Are QC test results reviewed for accuracy/compliance with specs (raw data, calculations verified, standards calibrated)?'),
    ('qu_disposition_5', 'Are all deviations, OOS investigations (closed with RCA), and changes linked to the batch reviewed/approved prior to release?'),
    ('qu_disposition_6', 'Are out-of-trend (OOT) results evaluated (statistical significance, trend investigation) even if within spec?'),
    ('qu_disposition_7', 'Are stability study commitments (pulls completed, data on schedule) reviewed before release for shelf-life batches?'),
    ('qu_disposition_8', 'Are in-process controls (IPC weights, pH, uniformity) verified against limits with justifications for excursions?'),
    ('qu_disposition_9', 'Are electronic batch records (eBMR) validated (Part 11 audit trails, signatures) and reviewed with exception highlighting?'),
    ('qu_disposition_10', 'Are standardized QA review checklists used consistently (100% batches, signed sections) covering 211.188 requirements?'),
    ('qu_disposition_11', 'Are rejected batches documented with justification (OOS, deviation impact), quarantine status, and disposition plan?'),
    ('qu_disposition_12', 'Is segregation between released and quarantined materials ensured (physical ERP status, double checks) with zero mix-ups?'),
    ('qu_disposition_13', 'Are line clearance records (previous batch removal, label verification) reviewed for packaging batches?'),
    ('qu_disposition_14', 'Are environmental monitoring (Grade A viable <1 CFU) and utility (WFI TOC <500ppb) excursion records examined for impact?'),
    ('qu_disposition_15', 'Are retain sample quantities verified sufficient (3x monograph + stability) and properly stored per SOP?'),
    ('qu_disposition_16', 'Are assigned expiry/retest periods verified against approved stability data/label claim before release?'),
    ('qu_disposition_17', 'Are open/recent market complaints reviewed for potential impact on current batch (lot similarity)?'),
    ('qu_disposition_18', 'Are CQAs (assay ≥98%, impurities <0.5%, dissolution ≥80%) verified with trending (Cpk≥1.33)?'),
    ('qu_disposition_19', 'Is sampling traceability verified (plan followed, chain-of-custody, composite/homogeneity)?'),
    ('qu_disposition_20', 'Are batch disposition timelines monitored (median ≤3 days post-QC results) with overdue escalations?'),
    ('qu_disposition_21', 'Are batch disposition decisions documented with electronic audit trail (Part 11 signatures, rationale attached)?'),
    ('qu_disposition_22', 'Are rework/reprocessing steps (protocol approved, yields reconciled, QC retest) reviewed/approved before final release?'),
    ('qu_disposition_23', 'Are printed packaging components verified for correctness (lot#, expiry, barcode scan, 100% check)?'),
    ('qu_disposition_24', 'Are trends in batch failures/rejects (Pareto by cause, rate <1%) reviewed quarterly?'),
    ('qu_disposition_25', 'Is batch disposition summary (release rates ≥99%, trends) included in management review discussions?'),
]

for i, (q_id, q_text) in enumerate(disposition_questions, 1):
    pattern = rf'Question\("{re.escape(q_id)}".*?\),'
    replacement = f'Question("{q_id}", "qu_disposition", "{q_text}", {i}),'
    content = re.sub(pattern, replacement, content, count=1)

print("Updated Disposition questions")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done updating APQR and Disposition")
