# Email Setup Guide for Validator App

## Current Status
✅ Email sending is integrated in the app for:
- Super Admin creating enterprises → Sends credentials to enterprise admin
- Super Admin creating standalone users → Sends credentials to user
- Enterprise Admin creating users → Sends credentials to user

Currently, the emails are **logged to console** but not actually sent. To send real emails, you need to set up an email service.

---

## Option 1: Firebase Extensions (Recommended - Easiest)

### Setup Steps:

1. **Go to Firebase Console**
   - Open [Firebase Console](https://console.firebase.google.com)
   - Select your project
   - Click **Extensions** in the left sidebar

2. **Install "Trigger Email" Extension**
   - Search for "Trigger Email"
   - Click **Install**
   - Follow the setup wizard

3. **Configure Email Service**
   - Choose your email provider:
     - **SendGrid** (Recommended - Free tier: 100 emails/day)
     - **Mailgun** (Free tier: 5,000 emails/month)
     - **SMTP** (Use your own email server)

4. **Get API Key**
   - For SendGrid:
     - Go to [SendGrid](https://sendgrid.com)
     - Sign up for free account
     - Go to Settings → API Keys
     - Create new API key
     - Copy the key

5. **Configure Extension**
   - Enter your API key
   - Set sender email (e.g., `noreply@yourapp.com`)
   - Set Firestore collection path: `mail`

6. **Update EmailService.kt**
   ```kotlin
   // In sendCredentialsEmail method, add:
   private suspend fun writeEmailToFirestore(
       toEmail: String,
       subject: String,
       content: String
   ) {
       val firestore = FirebaseFirestore.getInstance()
       val email = hashMapOf(
           "to" to toEmail,
           "message" to hashMapOf(
               "subject" to subject,
               "text" to content,
               "html" to content.replace("\n", "<br>")
           )
       )
       firestore.collection("mail").add(email).await()
   }
   
   // Then call it in sendCredentialsEmail:
   writeEmailToFirestore(toEmail, "Login Credentials - Validator", emailContent)
   ```

---

## Option 2: SendGrid API (Direct Integration)

### Setup Steps:

1. **Sign up for SendGrid**
   - Go to [SendGrid](https://sendgrid.com)
   - Create free account (100 emails/day)

2. **Get API Key**
   - Go to Settings → API Keys
   - Create new API key with "Mail Send" permission
   - Copy the key

3. **Add to build.gradle**
   ```gradle
   dependencies {
       implementation 'com.squareup.okhttp3:okhttp:4.12.0'
   }
   ```

4. **Create SendGrid Service**
   ```kotlin
   // In EmailService.kt
   private suspend fun sendViaSendGrid(
       toEmail: String,
       subject: String,
       content: String
   ): Result<String> = withContext(Dispatchers.IO) {
       try {
           val apiKey = "YOUR_SENDGRID_API_KEY" // TODO: Move to secure config
           
           val json = """
           {
               "personalizations": [{
                   "to": [{"email": "$toEmail"}]
               }],
               "from": {"email": "noreply@yourapp.com"},
               "subject": "$subject",
               "content": [{
                   "type": "text/plain",
                   "value": "$content"
               }]
           }
           """.trimIndent()
           
           val client = OkHttpClient()
           val request = Request.Builder()
               .url("https://api.sendgrid.com/v3/mail/send")
               .addHeader("Authorization", "Bearer $apiKey")
               .addHeader("Content-Type", "application/json")
               .post(json.toRequestBody("application/json".toMediaType()))
               .build()
           
           val response = client.newCall(request).execute()
           if (response.isSuccessful) {
               Result.success("Email sent successfully")
           } else {
               Result.failure(Exception("Failed to send email: ${response.code}"))
           }
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   
   // Then call it in sendCredentialsEmail:
   sendViaSendGrid(toEmail, "Login Credentials - Validator", emailContent)
   ```

---

## Option 3: Cloud Functions (Most Secure)

### Setup Steps:

1. **Install Firebase CLI**
   ```bash
   npm install -g firebase-tools
   firebase login
   ```

2. **Initialize Functions**
   ```bash
   cd /path/to/your/project
   firebase init functions
   ```

3. **Create Email Function**
   ```javascript
   // functions/index.js
   const functions = require('firebase-functions');
   const nodemailer = require('nodemailer');
   
   exports.sendCredentialsEmail = functions.https.onCall(async (data, context) => {
       const { toEmail, recipientName, tempPassword, enterpriseName } = data;
       
       // Configure your email service
       const transporter = nodemailer.createTransport({
           service: 'gmail', // or 'sendgrid', 'mailgun', etc.
           auth: {
               user: 'your-email@gmail.com',
               pass: 'your-app-password'
           }
       });
       
       const mailOptions = {
           from: 'noreply@yourapp.com',
           to: toEmail,
           subject: 'Login Credentials - Validator',
           text: `Hello ${recipientName}...\n\nEmail: ${toEmail}\nPassword: ${tempPassword}...`
       };
       
       await transporter.sendMail(mailOptions);
       return { success: true };
   });
   ```

4. **Deploy Function**
   ```bash
   firebase deploy --only functions
   ```

5. **Call from Android App**
   ```kotlin
   // Add to build.gradle
   implementation 'com.google.firebase:firebase-functions-ktx'
   
   // In EmailService.kt
   private suspend fun callEmailCloudFunction(
       toEmail: String,
       recipientName: String,
       tempPassword: String,
       enterpriseName: String
   ): Result<String> {
       return try {
           val functions = Firebase.functions
           val data = hashMapOf(
               "toEmail" to toEmail,
               "recipientName" to recipientName,
               "tempPassword" to tempPassword,
               "enterpriseName" to enterpriseName
           )
           
           functions.getHttpsCallable("sendCredentialsEmail")
               .call(data)
               .await()
           
           Result.success("Email sent successfully")
       } catch (e: Exception) {
           Result.failure(e)
       }
   }
   ```

---

## Recommendation

**For quick setup: Use Option 1 (Firebase Extensions)**
- No code changes needed
- Easy to configure
- Handles email delivery automatically
- Free tier available

**For production: Use Option 3 (Cloud Functions)**
- Most secure (API keys not in app)
- Better error handling
- Can customize email templates
- Scalable

---

## Testing

After setup, test by:
1. Creating a new enterprise as Super Admin
2. Check logs for email content
3. Check recipient's inbox
4. Verify email contains correct credentials

---

## Security Notes

⚠️ **IMPORTANT:**
- Never commit API keys to version control
- Use environment variables or Firebase Remote Config
- Implement rate limiting to prevent abuse
- Consider using email verification before sending credentials
- Log all email sending attempts for audit trail

---

## Current Email Template

The app sends this email format:

```
========================================
VALIDATOR - YOUR LOGIN CREDENTIALS
========================================

Hello [Name],

Your account has been created for the Validator app at [Enterprise].

Here are your login credentials:

Email: [email]
Temporary Password: [password]

IMPORTANT: This is a temporary password. For security reasons, 
you MUST change your password after your first login.

To get started:
1. Download and open the Validator app
2. Login with the credentials above
3. You will be prompted to change your password immediately
4. Create a strong, unique password

If you did not expect this email or have any questions, 
please contact your administrator.

Best regards,
The Validator Team
```

---

## Next Steps

1. Choose an email service option
2. Follow setup steps above
3. Update EmailService.kt with chosen method
4. Test thoroughly
5. Monitor email delivery rates
6. Set up email bounce handling (optional)

For questions or issues, refer to:
- [Firebase Extensions Docs](https://firebase.google.com/docs/extensions)
- [SendGrid API Docs](https://docs.sendgrid.com/)
- [Nodemailer Docs](https://nodemailer.com/)

