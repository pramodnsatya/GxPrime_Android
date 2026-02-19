# Firebase Configuration Setup

**⚠️ IMPORTANT: Never commit Firebase credentials to Git!**

This guide explains how to set up Firebase credentials for the GxPrime Android app.

## Required Files (Not in Repository)

The following files are required but intentionally excluded from Git for security:

### 1. `app/google-services.json`
Firebase configuration file for Android app.

**How to get it:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `validator-31e53`
3. Go to Project Settings → General
4. Under "Your apps" section, find the Android app
5. Click "Download google-services.json"
6. Place it in `app/google-services.json`

### 2. `serviceAccountKey.json` (Optional - for server operations)
Firebase Admin SDK service account key.

**How to get it:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: `validator-31e53`
3. Go to Project Settings → Service Accounts
4. Click "Generate new private key"
5. Save it as `serviceAccountKey.json` in the project root

### 3. `local.properties`
Contains OpenAI API key for AI features.

**Create this file in project root:**
```properties
sdk.dir=/path/to/Android/sdk
OPENAI_API_KEY=your_openai_api_key_here
```

## Setup Steps

1. **Clone the repository:**
   ```bash
   git clone https://github.com/pramodnsatya/GxPrime_Android.git
   cd GxPrime_Android
   ```

2. **Add Firebase configuration:**
   - Download `google-services.json` (see above)
   - Place in `app/` directory
   - Verify it's in `.gitignore`

3. **Add OpenAI API Key:**
   - Create `local.properties` file
   - Add your OpenAI API key
   - Never commit this file

4. **Build the app:**
   ```bash
   ./gradlew assembleDebug
   ```

## Security Checklist

Before committing any changes:

- [ ] `google-services.json` is NOT in git
- [ ] `serviceAccountKey.json` is NOT in git  
- [ ] `local.properties` is NOT in git
- [ ] No API keys in source code
- [ ] All secrets are in `.gitignore`

## Firestore Security Rules

Ensure proper security rules are configured in Firebase Console:
- Go to Firestore Database → Rules
- Apply role-based access control rules
- See project documentation for complete rules

## Need Help?

If you need access to Firebase credentials:
1. Contact the project admin
2. Request access to Firebase project
3. Follow the setup steps above

---

**Remember: Security is everyone's responsibility. Never commit credentials!**
