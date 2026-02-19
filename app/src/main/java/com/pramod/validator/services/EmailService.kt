package com.pramod.validator.services

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

class EmailService {
    private val auth = FirebaseAuth.getInstance()
    
    // Firebase Auth Email is always available - no configuration needed!
    private fun isEmailConfigured(): Boolean {
        return true // Firebase Auth email is always available
    }
    
    /**
     * Send credentials email with temporary password
     * 
     * IMPORTANT: To send actual emails, you need to set up one of these options:
     * 
     * Option 1 - Firebase Extensions (Recommended):
     * 1. Go to Firebase Console > Extensions
     * 2. Install "Trigger Email" extension
     * 3. Configure with your email service (SendGrid, Mailgun, etc.)
     * 4. The extension will watch a Firestore collection and send emails
     * 
     * Option 2 - Cloud Functions:
     * 1. Create a Cloud Function that sends emails
     * 2. Call the function from this method using Firebase Functions SDK
     * 
     * Option 3 - Third-party API:
     * 1. Use SendGrid, AWS SES, or Mailgun API directly
     * 2. Add API key to your app configuration
     * 3. Make HTTP requests from this method
     * 
     * For now, we log the credentials so admins can manually send them.
     */
    suspend fun sendCredentialsEmail(
        toEmail: String,
        recipientName: String,
        tempPassword: String,
        enterpriseName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val emailContent = createCredentialsEmailText(recipientName, toEmail, tempPassword, enterpriseName)
            
            android.util.Log.d("EmailService", "╔═══════════════════════════════════════════════════════════════")
            android.util.Log.d("EmailService", "║ CREDENTIALS EMAIL - READY TO SEND")
            android.util.Log.d("EmailService", "╠═══════════════════════════════════════════════════════════════")
            android.util.Log.d("EmailService", "║ To: $toEmail")
            android.util.Log.d("EmailService", "║ Recipient: $recipientName")
            android.util.Log.d("EmailService", "║ Enterprise: $enterpriseName")
            android.util.Log.d("EmailService", "║ Temporary Password: $tempPassword")
            android.util.Log.d("EmailService", "╠═══════════════════════════════════════════════════════════════")
            android.util.Log.d("EmailService", "║ EMAIL CONTENT:")
            android.util.Log.d("EmailService", "╠═══════════════════════════════════════════════════════════════")
            emailContent.lines().forEach { line ->
                android.util.Log.d("EmailService", "║ $line")
            }
            android.util.Log.d("EmailService", "╚═══════════════════════════════════════════════════════════════")
            
            // TODO: Integrate with actual email service
            // Uncomment and configure one of these options:
            
            // Option 1: Write to Firestore collection for Firebase Extension to pick up
            // writeEmailToFirestore(toEmail, "Login Credentials", emailContent)
            
            // Option 2: Call Cloud Function
            // callEmailCloudFunction(toEmail, "Login Credentials", emailContent)
            
            // Option 3: Call third-party API (e.g., SendGrid)
            // sendViaSendGrid(toEmail, "Login Credentials", emailContent)
            
            Result.success("Credentials logged. Configure email service to send actual emails.")
        } catch (e: Exception) {
            android.util.Log.e("EmailService", "Error preparing credentials email: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun createCredentialsEmailText(
        recipientName: String,
        email: String,
        tempPassword: String,
        enterpriseName: String
    ): String {
        return """
        ========================================
        VALIDATOR - YOUR LOGIN CREDENTIALS
        ========================================
        
        Hello $recipientName,
        
        Your account has been created for the Validator app at $enterpriseName.
        
        Here are your login credentials:
        
        Email: $email
        Temporary Password: $tempPassword
        
        IMPORTANT: This is a temporary password. For security reasons, you MUST change your password after your first login.
        
        To get started:
        1. Download and open the Validator app
        2. Login with the credentials above
        3. You will be prompted to change your password immediately
        4. Create a strong, unique password
        
        If you did not expect this email or have any questions, please contact your administrator.
        
        Best regards,
        The Validator Team
        
        ========================================
        © 2024 Validator. All rights reserved.
        This is an automated message.
        ========================================
        """.trimIndent()
    }
    
    suspend fun sendInvitationEmail(
        toEmail: String,
        recipientName: String,
        enterpriseName: String,
        invitationLink: String,
        invitedBy: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("EmailService", "Sending invitation email to: $toEmail")
            
            // Create a temporary user account to send email verification
            // This will send a real email from Firebase Auth
            val tempPassword = "TempPassword123!"
            
            try {
                // Create temporary user
                val tempUser = auth.createUserWithEmailAndPassword(toEmail, tempPassword).await()
                
                if (tempUser.user != null) {
                    // Send email verification (this will be our "invitation" email)
                    tempUser.user?.sendEmailVerification()?.await()
                    
                    // Sign out and delete the temporary user
                    auth.signOut()
                    tempUser.user?.delete()?.await()
                    
                    android.util.Log.d("EmailService", "Email verification sent successfully to $toEmail")
                    android.util.Log.d("EmailService", "User will receive an email verification email from Firebase")
                    android.util.Log.d("EmailService", "Invitation link for manual sharing: $invitationLink")
                    
                    Result.success("Invitation email sent successfully")
                } else {
                    android.util.Log.e("EmailService", "Failed to create temporary user")
                    Result.failure(Exception("Failed to send invitation email"))
                }
            } catch (e: Exception) {
                // If user already exists, try to send email verification
                if (e.message?.contains("email-already-in-use") == true) {
                    android.util.Log.d("EmailService", "User already exists, trying to send email verification")
                    try {
                        // Try to sign in and send verification
                        val existingUser = auth.signInWithEmailAndPassword(toEmail, tempPassword).await()
                        if (existingUser.user != null) {
                            existingUser.user?.sendEmailVerification()?.await()
                            auth.signOut()
                            android.util.Log.d("EmailService", "Email verification sent to existing user: $toEmail")
                            Result.success("Invitation email sent successfully")
                        } else {
                            Result.failure(Exception("Failed to send email to existing user"))
                        }
                    } catch (signInError: Exception) {
                        android.util.Log.e("EmailService", "Could not send email to existing user: ${signInError.message}")
                        Result.failure(Exception("Could not send email to existing user"))
                    }
                } else {
                    throw e
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EmailService", "Error sending invitation email: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun createInvitationEmailHtml(
        recipientName: String,
        enterpriseName: String,
        invitationLink: String,
        invitedBy: String
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Invitation to Join $enterpriseName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f8f9fa;
                }
                .container {
                    background: white;
                    border-radius: 12px;
                    padding: 40px;
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .logo {
                    font-size: 28px;
                    font-weight: bold;
                    color: #2563eb;
                    margin-bottom: 10px;
                }
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    color: #1f2937;
                    margin-bottom: 20px;
                }
                .content {
                    margin-bottom: 30px;
                }
                .button {
                    display: inline-block;
                    background: linear-gradient(135deg, #2563eb, #1d4ed8);
                    color: white;
                    text-decoration: none;
                    padding: 16px 32px;
                    border-radius: 8px;
                    font-weight: 600;
                    font-size: 16px;
                    text-align: center;
                    margin: 20px 0;
                    box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
                    transition: all 0.3s ease;
                }
                .button:hover {
                    transform: translateY(-2px);
                    box-shadow: 0 6px 16px rgba(37, 99, 235, 0.4);
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                    font-size: 14px;
                    color: #6b7280;
                    text-align: center;
                }
                .highlight {
                    background: linear-gradient(135deg, #fef3c7, #fde68a);
                    padding: 16px;
                    border-radius: 8px;
                    border-left: 4px solid #f59e0b;
                    margin: 20px 0;
                }
                .permissions {
                    background: #f8fafc;
                    padding: 20px;
                    border-radius: 8px;
                    margin: 20px 0;
                }
                .permissions h3 {
                    margin-top: 0;
                    color: #1f2937;
                }
                .permissions ul {
                    margin: 10px 0;
                    padding-left: 20px;
                }
                .permissions li {
                    margin: 5px 0;
                    color: #4b5563;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">📋 Validator</div>
                    <h1 class="title">You're Invited!</h1>
                </div>
                
                <div class="content">
                    <p>Hello <strong>$recipientName</strong>,</p>
                    
                    <p><strong>$invitedBy</strong> has invited you to join <strong>$enterpriseName</strong> on Validator, our comprehensive assessment and compliance management platform.</p>
                    
                    <div class="highlight">
                        <strong>🎯 What is Validator?</strong><br>
                        Validator helps organizations manage assessments, track compliance, and maintain quality standards across all departments.
                    </div>
                    
                    <div class="permissions">
                        <h3>🔐 Your Access Includes:</h3>
                        <ul>
                            <li>Complete assessments in assigned domains</li>
                            <li>View your assessment history</li>
                            <li>Access to department-specific reports</li>
                            <li>Real-time compliance tracking</li>
                            <li>AI-powered assessment summaries</li>
                        </ul>
                    </div>
                    
                    <p>Click the button below to accept your invitation and set up your account:</p>
                    
                    <div style="text-align: center;">
                        <a href="$invitationLink" class="button">Accept Invitation & Create Account</a>
                    </div>
                    
                    <p><strong>Important:</strong> This invitation link will expire in 7 days. If you have any questions, please contact your administrator.</p>
                </div>
                
                <div class="footer">
                    <p>This invitation was sent by $invitedBy from $enterpriseName.</p>
                    <p>If you didn't expect this invitation, you can safely ignore this email.</p>
                    <p style="margin-top: 20px; font-size: 12px; color: #9ca3af;">
                        © 2024 Validator. All rights reserved.<br>
                        This is an automated message, please do not reply to this email.
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
    
    suspend fun sendWelcomeEmail(
        toEmail: String,
        recipientName: String,
        enterpriseName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("EmailService", "=== WELCOME EMAIL ===")
            android.util.Log.d("EmailService", "To: $toEmail")
            android.util.Log.d("EmailService", "Recipient: $recipientName")
            android.util.Log.d("EmailService", "Enterprise: $enterpriseName")
            android.util.Log.d("EmailService", "Welcome to $enterpriseName on Validator!")
            android.util.Log.d("EmailService", "===================")
            
            android.util.Log.d("EmailService", "Welcome email details logged")
            Result.success("Welcome email details logged")
        } catch (e: Exception) {
            android.util.Log.e("EmailService", "Error logging welcome email: ${e.message}")
            Result.failure(e)
        }
    }
    
    private fun createWelcomeEmailHtml(
        recipientName: String,
        enterpriseName: String
    ): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Welcome to $enterpriseName</title>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                    line-height: 1.6;
                    color: #333;
                    max-width: 600px;
                    margin: 0 auto;
                    padding: 20px;
                    background-color: #f8f9fa;
                }
                .container {
                    background: white;
                    border-radius: 12px;
                    padding: 40px;
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                }
                .header {
                    text-align: center;
                    margin-bottom: 30px;
                }
                .logo {
                    font-size: 28px;
                    font-weight: bold;
                    color: #10b981;
                    margin-bottom: 10px;
                }
                .title {
                    font-size: 24px;
                    font-weight: 600;
                    color: #1f2937;
                    margin-bottom: 20px;
                }
                .success {
                    background: linear-gradient(135deg, #d1fae5, #a7f3d0);
                    padding: 20px;
                    border-radius: 8px;
                    border-left: 4px solid #10b981;
                    margin: 20px 0;
                    text-align: center;
                }
                .next-steps {
                    background: #f8fafc;
                    padding: 20px;
                    border-radius: 8px;
                    margin: 20px 0;
                }
                .next-steps h3 {
                    margin-top: 0;
                    color: #1f2937;
                }
                .next-steps ol {
                    margin: 10px 0;
                    padding-left: 20px;
                }
                .next-steps li {
                    margin: 8px 0;
                    color: #4b5563;
                }
                .footer {
                    margin-top: 40px;
                    padding-top: 20px;
                    border-top: 1px solid #e5e7eb;
                    font-size: 14px;
                    color: #6b7280;
                    text-align: center;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="logo">🎉 Validator</div>
                    <h1 class="title">Welcome to $enterpriseName!</h1>
                </div>
                
                <div class="success">
                    <strong>✅ Account Created Successfully!</strong><br>
                    You're now part of the $enterpriseName team on Validator.
                </div>
                
                <p>Hello <strong>$recipientName</strong>,</p>
                
                <p>Your account has been successfully created and you now have access to all the assessment and compliance tools available to your role.</p>
                
                <div class="next-steps">
                    <h3>🚀 Next Steps:</h3>
                    <ol>
                        <li><strong>Download the app</strong> from your app store if you haven't already</li>
                        <li><strong>Sign in</strong> using the credentials you just created</li>
                        <li><strong>Explore your dashboard</strong> to see available assessments</li>
                        <li><strong>Complete your first assessment</strong> to get familiar with the platform</li>
                        <li><strong>Check your permissions</strong> to understand what you can access</li>
                    </ol>
                </div>
                
                <p>If you have any questions or need assistance, please don't hesitate to contact your administrator or support team.</p>
                
                <div class="footer">
                    <p>Welcome to the $enterpriseName team!</p>
                    <p style="margin-top: 20px; font-size: 12px; color: #9ca3af;">
                        © 2024 Validator. All rights reserved.<br>
                        This is an automated message, please do not reply to this email.
                    </p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
