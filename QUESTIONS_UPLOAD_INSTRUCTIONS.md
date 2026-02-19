# How to Upload Questions to Firebase

## ✅ STATUS: ALL QUALITY UNIT QUESTIONS UPDATED & BUILD SUCCESSFUL

All 18 Quality Unit subdomains (450 questions) have been successfully updated in `QualityUnitQuestions.kt` with content from your PDF files. The app builds successfully and is ready for Firebase upload.

## What Was Updated

**All 18 Quality Unit Subdomains - 450 Questions Total**:

1. ✅ **Deviations** (1.1) - 25 questions
2. ✅ **Investigations** (1.2) - 25 questions  
3. ✅ **CAPA** (1.3) - 25 questions
4. ✅ **Document Management** (1.4) - 25 questions
5. ✅ **Complaint Management** (1.5) - 25 questions
6. ✅ **Quality Risk Management** (1.6) - 25 questions
7. ✅ **Data Integrity Governance** (1.7) - 25 questions
8. ✅ **Training Management** (1.8) - 25 questions
9. ✅ **Field Alert Reports** (1.9) - 25 questions
10. ✅ **Change Control** (1.10) - 25 questions
11. ✅ **Returned & Salvaged Drug Products** (1.11) - 25 questions
12. ✅ **Audit Management** (1.12) - 25 questions
13. ✅ **Computer System Validation** (1.13) - 25 questions
14. ✅ **Technology Transfer Oversight** (1.14) - 25 questions
15. ✅ **Annual Product Quality Review** (1.15) - 25 questions
16. ✅ **Product Disposition Release-Rejection** (1.16) - 25 questions
17. ✅ **Management Review & Quality Metrics** (1.17) - 25 questions
18. ✅ **Supplier Quality Oversight** (1.18) - 25 questions

## Primary Method: Use the App (Recommended)

1. **Build and run the app** on a device or emulator:
   ```bash
   ./gradlew assembleDebug
   ```

2. **Log in as Super Admin** (using your super admin email)

3. **Navigate to the Super Admin Dashboard**

4. **Scroll down** to the "Assessment Questions" section

5. **Click "Upload All Questions"** button

This will upload all 450 updated Quality Unit questions to Firebase Firestore. The app will replace existing questions with the new ones.

## Verification

After uploading:
1. Log in as a regular user (or Enterprise Admin)
2. Start an assessment for any Quality Unit subdomain (e.g., Deviations, CAPA, Training Management)
3. Verify the new detailed questions appear with proper formatting

## Alternative: Node.js Script (If App Upload Fails)

If the app upload doesn't work (e.g., timeout with large dataset):

1. Create a Firebase service account key:
   - Firebase Console → Project Settings → Service Accounts → Generate new private key
   - Save the JSON file securely

2. Run the script:
   ```bash
   GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account-key.json node scripts/upload-questions.js
   ```

Note: The script currently uploads a small sample. For full upload, use the app method.

## Firebase Project Details

- **Project ID**: `validator-31e53`
- **Database**: Firestore
- **Collection**: `questions`
- **Document ID Format**: `{subdomainId}_{number}` (e.g., `qu_deviations_1`, `qu_capa_15`)

## Important Notes

- The Firebase Firestore database will automatically overwrite existing questions with the same IDs
- All users will see the updated questions immediately after upload
- Questions are stored with: `id`, `domainId` (used for subDomainId), `text`, and `order` fields
- The app has been verified to build successfully with all updates

## Next Steps

1. Use the app's "Upload All Questions" button to push the updated questions to Firebase
2. Verify that users can see the new comprehensive questions in the app
3. All done! 🎉
