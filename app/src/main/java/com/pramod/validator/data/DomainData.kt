package com.pramod.validator.data

import com.pramod.validator.data.models.Domain
import com.pramod.validator.data.models.SubDomain

object DomainData {
    
    fun getDomains(): List<Domain> {
        return listOf(
            Domain("quality_unit", "Quality Unit", "Quality management and oversight", "🎯", 1),
            Domain("packaging_labeling", "Packaging & Labeling", "Packaging operations and controls", "📦", 2),
            Domain("production", "Production", "Manufacturing and process control", "🏭", 3),
            Domain("materials", "Materials", "Material management and supply chain", "📋", 4),
            Domain("laboratory", "Laboratory Systems", "Testing and quality control", "🔬", 5),
            Domain("facilities", "Facilities & Equipment", "Infrastructure and equipment management", "🏢", 6)
        )
    }
    
    fun getSubDomains(domainId: String): List<SubDomain> {
        return when (domainId) {
            "quality_unit" -> listOf(
                SubDomain("qu_deviations", "quality_unit", "Deviations", "Management of deviations from procedures", 1),
                SubDomain("qu_investigations", "quality_unit", "Investigations", "Root cause analysis and investigations", 2),
                SubDomain("qu_capa", "quality_unit", "CAPA", "Corrective and Preventive Actions", 3),
                SubDomain("qu_document_mgmt", "quality_unit", "Document Management", "Document control and management", 4),
                SubDomain("qu_complaint_mgmt", "quality_unit", "Complaint Management", "Customer complaint handling", 5),
                SubDomain("qu_risk_mgmt", "quality_unit", "Quality Risk Management", "Risk assessment and mitigation", 6),
                SubDomain("qu_data_integrity", "quality_unit", "Data Integrity Governance", "Data integrity controls", 7),
                SubDomain("qu_training", "quality_unit", "Training Management", "Personnel training and qualification", 8),
                SubDomain("qu_field_alerts", "quality_unit", "Field Alert Reports", "Product alerts and recalls", 9),
                SubDomain("qu_change_control", "quality_unit", "Change Control", "Change management process", 10),
                SubDomain("qu_returned_drugs", "quality_unit", "Returned and Salvaged Drug Products", "Handling returned products", 11),
                SubDomain("qu_audit", "quality_unit", "Audit Management", "Internal and external audits", 12),
                SubDomain("qu_csv", "quality_unit", "Computer System Validation", "System validation and compliance", 13),
                SubDomain("qu_tech_transfer", "quality_unit", "Technology Transfer Oversight", "Process and technology transfer", 14),
                SubDomain("qu_apqr", "quality_unit", "Annual Product Quality Review (APQR)", "Annual product reviews", 15),
                SubDomain("qu_disposition", "quality_unit", "Product Disposition (Release/Rejection)", "Product release decisions", 16),
                SubDomain("qu_mgmt_review", "quality_unit", "Management Review & Quality Metrics", "Management review meetings", 17),
                SubDomain("qu_supplier", "quality_unit", "Supplier Quality Oversight", "Supplier quality assurance", 18)
            )
            
            "packaging_labeling" -> listOf(
                SubDomain("pl_label_control", "packaging_labeling", "Label and Packaging Control", "Label and packaging material control", 1),
                SubDomain("pl_line_setup", "packaging_labeling", "Packaging Line Setup", "Packaging line configuration", 2),
                SubDomain("pl_cold_chain", "packaging_labeling", "Cold Chain Management", "Temperature-controlled packaging", 3),
                SubDomain("pl_primary", "packaging_labeling", "Primary Packaging", "Direct product packaging", 4),
                SubDomain("pl_validation", "packaging_labeling", "Packaging Validation", "Validation of packaging processes", 5),
                SubDomain("pl_serialization", "packaging_labeling", "Serialization", "Product serialization and tracking", 6),
                SubDomain("pl_artwork", "packaging_labeling", "Artwork Management", "Packaging artwork control", 7),
                SubDomain("pl_line_clearance", "packaging_labeling", "Line Clearance", "Line clearance procedures", 8),
                SubDomain("pl_returned_goods", "packaging_labeling", "Returned Goods Handling", "Handling returned packaged products", 9),
                SubDomain("pl_tamper_evident", "packaging_labeling", "Tamper-Evident Packaging", "Tamper-evident features", 10),
                SubDomain("pl_secondary", "packaging_labeling", "Secondary Packaging", "Outer packaging controls", 11),
                SubDomain("pl_reconciliation", "packaging_labeling", "Label Reconciliation", "Label usage reconciliation", 12),
                SubDomain("pl_records", "packaging_labeling", "Packaging Records Review", "Review of packaging records", 13)
            )
            
            "production" -> listOf(
                SubDomain("pr_contamination", "production", "Contamination Control", "Prevention of cross-contamination", 1),
                SubDomain("pr_process_val", "production", "Process Validation", "Manufacturing process validation", 2),
                SubDomain("pr_cleaning_val", "production", "Cleaning Validation", "Cleaning procedure validation", 3),
                SubDomain("pr_batch_records", "production", "Batch Records", "Batch production records", 4),
                SubDomain("pr_media_fills", "production", "Media Fills", "Aseptic process simulation", 5),
                SubDomain("pr_batch_release", "production", "Batch Release", "Batch disposition and release", 6),
                SubDomain("pr_process_control", "production", "Process Control", "In-process controls", 7),
                SubDomain("pr_monitoring", "production", "Process Monitoring", "Continuous process monitoring", 8),
                SubDomain("pr_manufacturing", "production", "Batch Manufacturing", "Production operations", 9),
                SubDomain("pr_retain_samples", "production", "Retain Samples", "Retention sample management", 10),
                SubDomain("pr_potent_drugs", "production", "Handling of Highly Potent & Sensitizing Drugs", "Special handling requirements", 11),
                SubDomain("pr_ipc", "production", "In-Process Controls (IPC)", "In-process testing", 12),
                SubDomain("pr_master_records", "production", "Master Production Records & Instructions", "Master batch records", 13),
                SubDomain("pr_traceability", "production", "Material Traceability & Reconciliation", "Material tracking", 14)
            )
            
            "materials" -> listOf(
                SubDomain("mt_supplier_mgmt", "materials", "Supplier Quality Management", "Supplier performance management", 1),
                SubDomain("mt_qualification", "materials", "Supplier Qualification & Requalification", "Supplier approval process", 2),
                SubDomain("mt_storage", "materials", "Material Storage and Warehousing Control", "Warehouse management", 3),
                SubDomain("mt_sampling", "materials", "Material Sampling and Testing", "Raw material sampling", 4),
                SubDomain("mt_receipt", "materials", "Material Receipt and Handling", "Receiving procedures", 5),
                SubDomain("mt_distribution", "materials", "Distribution", "Product distribution control", 6),
                SubDomain("mt_quarantine", "materials", "Quarantine Management", "Quarantine area control", 7),
                SubDomain("mt_reconciliation", "materials", "Material Reconciliation", "Material balance reconciliation", 8)
            )
            
            "laboratory" -> listOf(
                SubDomain("lb_testing", "laboratory", "Sample Testing", "Analytical testing procedures", 1),
                SubDomain("lb_oos_oot", "laboratory", "OOS/OOT Investigations", "Out-of-specification investigations", 2),
                SubDomain("lb_stability", "laboratory", "Stability Studies", "Stability testing programs", 3),
                SubDomain("lb_sample_mgmt", "laboratory", "Sample Management", "Sample handling and storage", 4),
                SubDomain("lb_controls", "laboratory", "Laboratory Controls", "Lab operation controls", 5),
                SubDomain("lb_method_val", "laboratory", "Method Validation/Verification", "Analytical method validation", 6),
                SubDomain("lb_systems", "laboratory", "System Controls (LIMS, Chromatography, Audit Trails)", "Laboratory systems", 7),
                SubDomain("lb_penicillin", "laboratory", "Penicillin Contamination Control", "Penicillin-specific controls", 8),
                SubDomain("lb_reagents", "laboratory", "Reagents and Standards", "Reagent management", 9),
                SubDomain("lb_ref_standards", "laboratory", "Reference Standards Qualification", "Reference material qualification", 10),
                SubDomain("lb_trending", "laboratory", "Trending of Analytical Results", "Data trending analysis", 11),
                SubDomain("lb_data_integrity", "laboratory", "Data Integrity in Laboratory Systems", "Lab data integrity", 12)
            )
            
            "facilities" -> listOf(
                SubDomain("fc_env_monitoring", "facilities", "Environmental Monitoring", "Clean room monitoring", 1),
                SubDomain("fc_eq_qual", "facilities", "Equipment Qualification (DQ/IQ/OQ/PQ)", "Equipment qualification", 2),
                SubDomain("fc_design", "facilities", "Facility Design & Flow (Personnel, Material)", "Facility layout design", 3),
                SubDomain("fc_eq_cleaning", "facilities", "Equipment Cleaning & Maintenance", "Equipment maintenance", 4),
                SubDomain("fc_area_cleaning", "facilities", "Area/Facility Cleaning", "Facility cleaning procedures", 5),
                SubDomain("fc_prev_maint", "facilities", "Preventive Maintenance", "Preventive maintenance program", 6),
                SubDomain("fc_qualification", "facilities", "Facility Qualification (HVAC, Cleanrooms)", "Facility qualification", 7),
                SubDomain("fc_calibration", "facilities", "Calibration Management", "Calibration program", 8),
                SubDomain("fc_water", "facilities", "Water Quality Monitoring (Purified, WFI)", "Water system monitoring", 9),
                SubDomain("fc_utilities", "facilities", "Utilities (Compressed Gases, Steam)", "Utility systems", 10),
                SubDomain("fc_pest", "facilities", "Pest Control", "Pest control program", 11),
                SubDomain("fc_alarms", "facilities", "Alarm Management", "Alarm system management", 12),
                SubDomain("fc_logbooks", "facilities", "Asset & Equipment Logbook Management", "Equipment logbooks", 13),
                SubDomain("fc_waste", "facilities", "Waste Management", "Waste disposal procedures", 14),
                SubDomain("fc_hvac", "facilities", "HVAC Systems", "HVAC operation and control", 15),
                SubDomain("fc_gowning", "facilities", "Gowning & Personnel Flow Control", "Gowning procedures", 16)
            )
            
            else -> emptyList()
        }
    }
}

