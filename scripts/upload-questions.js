#!/usr/bin/env node
/**
 * Upload Quality Unit questions to Firebase Firestore
 * 
 * Prerequisites:
 * 1. Create a service account key in Firebase Console:
 *    - Go to Project Settings > Service Accounts > Generate new private key
 * 2. Save the JSON file (e.g., as service-account-key.json) in a secure location
 * 
 * Usage:
 *   GOOGLE_APPLICATION_CREDENTIALS=path/to/service-account-key.json node scripts/upload-questions.js
 * 
 * Or specify project and key path:
 *   node scripts/upload-questions.js --project=validator-31e53 --key=path/to/key.json
 */

const admin = require('firebase-admin');
const path = require('path');

// Parse command line args
const args = process.argv.slice(2);
let projectId = 'validator-31e53';
let keyPath = process.env.GOOGLE_APPLICATION_CREDENTIALS;

args.forEach(arg => {
  if (arg.startsWith('--project=')) projectId = arg.split('=')[1];
  if (arg.startsWith('--key=')) keyPath = arg.split('=')[1];
});

// Initialize Firebase Admin
if (keyPath) {
  const keyPathResolved = path.resolve(keyPath);
  const serviceAccount = require(keyPathResolved);
  admin.initializeApp({ credential: admin.credential.cert(serviceAccount), projectId });
} else {
  admin.initializeApp({ projectId });
}

const db = admin.firestore();
const QUESTIONS_COLLECTION = 'questions';

// Quality Unit questions from PDFs (1.1-1.18) - subset for script size
// Full list is in QualityUnitQuestions.kt - this script is for manual upload when app button doesn't work
const QUALITY_UNIT_QUESTIONS = [
  // Deviations (1.1) - already updated in Kotlin
  { id: 'qu_deviations_1', domainId: 'qu_deviations', text: 'Does the site have an approved Standard Operating Procedure (SOP) for deviations that defines written classification criteria for minor, major, and critical deviations (for example, including specific examples for each category)?', order: 1 },
  // Add more as needed - run from app for full upload
];

async function uploadQuestions() {
  console.log('Uploading questions to Firebase...');
  const batch = db.batch();
  let count = 0;

  // Note: This script uploads a small set. For full upload, use the app:
  // Super Admin Dashboard > Assessment Questions > Upload All Questions
  console.log('To upload ALL questions, use the app: Log in as Super Admin, go to Dashboard, scroll to "Assessment Questions", click "Upload All Questions".');
  console.log('');
  console.log('Uploading sample questions for verification...');

  for (const q of QUALITY_UNIT_QUESTIONS) {
    const docRef = db.collection(QUESTIONS_COLLECTION).doc(q.id);
    batch.set(docRef, { id: q.id, domainId: q.domainId, text: q.text, order: q.order });
    count++;
  }

  await batch.commit();
  console.log(`Successfully uploaded ${count} questions.`);
  process.exit(0);
}

uploadQuestions().catch(err => {
  console.error('Upload failed:', err.message);
  process.exit(1);
});
