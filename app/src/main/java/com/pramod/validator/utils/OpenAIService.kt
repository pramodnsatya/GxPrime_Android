package com.pramod.validator.utils

import com.pramod.validator.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAIService {
    
    // Read API key from BuildConfig (set via local.properties)
    private val API_KEY: String = if (BuildConfig.OPENAI_API_KEY.isNotEmpty()) {
        BuildConfig.OPENAI_API_KEY
    } else {
        android.util.Log.w("OpenAIService", "⚠️ OPENAI_API_KEY not found in BuildConfig. AI features will not work.")
        ""
    }
    private const val API_URL = "https://api.openai.com/v1/chat/completions"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    fun generateAssessmentSummary(
        domainName: String,
        subDomainName: String,
        assessmentName: String,
        questionsAndAnswers: Map<String, Pair<String, String>>
    ): Result<String> {
        return try {
            // Validate API key
            if (API_KEY.isEmpty()) {
                android.util.Log.e("OpenAIService", "❌ OpenAI API key is not configured")
                return Result.failure(Exception("OpenAI API key is not configured. Please add OPENAI_API_KEY to local.properties"))
            }
            
            android.util.Log.d("OpenAIService", "🚀 Starting OpenAI API call for $subDomainName assessment")
            // Build the prompt
            val prompt = buildPrompt(domainName, subDomainName, assessmentName, questionsAndAnswers)
            
            // Create JSON request body
            val jsonBody = JSONObject().apply {
                // Use a faster, cheaper model for lower latency
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a GxPrime compliance expert. Always address the organization as 'your company'. Be concise and actionable.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                // Ask for structured JSON output to avoid markdown/asterisks in UI
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.4)
                put("max_tokens", 600)
            }
            
            // Create request
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            android.util.Log.d("OpenAIService", "📡 OpenAI API response code: ${response.code}")
            
            if (response.isSuccessful && responseBody != null) {
                android.util.Log.d("OpenAIService", "✅ OpenAI API call successful")
                // Parse response
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val summary = message.getString("content")
                    android.util.Log.d("OpenAIService", "📝 Received AI summary: ${summary.take(100)}...")
                    Result.success(summary.trim())
                } else {
                    android.util.Log.e("OpenAIService", "❌ No choices in OpenAI response")
                    val error = Exception("No response from OpenAI")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_assessment_summary")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "no_choices_in_response")
                    com.pramod.validator.utils.CrashReporting.logException(error, "OpenAI API returned no choices")
                    Result.failure(error)
                }
            } else {
                android.util.Log.e("OpenAIService", "❌ OpenAI API error: ${response.code} - ${responseBody}")
                val error = Exception("API Error: ${response.code} - ${responseBody ?: "No response body"}")
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_assessment_summary")
                com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "api_error")
                com.pramod.validator.utils.CrashReporting.setCustomKey("http_code", response.code.toString())
                com.pramod.validator.utils.CrashReporting.logException(error, "OpenAI API error: ${response.code}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "generate_assessment_summary")
            com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "exception")
            com.pramod.validator.utils.CrashReporting.logException(e, "Exception during OpenAI API call")
            Result.failure(e)
        }
    }
    
    private fun buildPrompt(
        domainName: String,
        subDomainName: String,
        assessmentName: String,
        questionsAndAnswers: Map<String, Pair<String, String>>
    ): String {
        val compliantCount = questionsAndAnswers.values.count { it.second == "COMPLIANT" }
        val nonCompliantCount = questionsAndAnswers.values.count { it.second == "NON_COMPLIANT" }
        val notApplicableCount = questionsAndAnswers.values.count { it.second == "NOT_APPLICABLE" }
        
        val qa = questionsAndAnswers.entries.joinToString("\n\n") { (_, pair) ->
            val (question, answer) = pair
            "Q: $question\nA: ${formatAnswer(answer)}"
        }
        
        // Ask for concise JSON with specific sections
        return """
Return a concise JSON with exactly these keys: 
{
  "strengths": string[],  // 2-4 short bullet points
  "issues": [             // focus on non-compliance; 2-5 items
    {
      "area": string,     // where the problem is
      "problem": string,  // what is wrong
      "improvement": string, // how to fix (actionable)
      "where": string,    // process/location/system
      "how": string       // concrete steps/tools/standards
    }
  ],
  "next_steps": string[]  // 3-5 immediate prioritized actions
}
Constraints: Address as "your company" (not "the company"). 120–180 words total. Be direct and practical. Use plain text only - no markdown, no asterisks, no emojis, no special characters.

Assessment:
- Name: $assessmentName
- Domain: $domainName
- Sub-Domain: $subDomainName
- Totals: compliant=$compliantCount, non_compliant=$nonCompliantCount, not_applicable=$notApplicableCount

Questions and Responses (summarized):
$qa
        """.trimIndent()
    }
    
    private fun formatAnswer(answer: String): String {
        return when (answer) {
            "COMPLIANT" -> "Compliant"
            "NON_COMPLIANT" -> "Non-Compliant"
            "NOT_APPLICABLE" -> "Not Applicable"
            else -> answer
        }
    }
    
    /**
     * Analyze FDA 483 document and generate risk areas, details, and checklist
     */
    fun analyzeFda483(pdfText: String): Result<String> {
        return try {
            // Validate API key
            if (API_KEY.isEmpty()) {
                android.util.Log.e("OpenAIService", "❌ OpenAI API key is not configured")
                val error = Exception("OpenAI API key is not configured. Please add OPENAI_API_KEY to local.properties")
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "analyze_fda483")
                com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "missing_api_key")
                com.pramod.validator.utils.CrashReporting.logException(error, "OpenAI API key not configured for FDA 483")
                return Result.failure(error)
            }
            
            android.util.Log.d("OpenAIService", "🚀 Starting FDA 483 analysis")
            
            val prompt = buildFda483Prompt(pdfText)
            
            val jsonBody = JSONObject().apply {
                put("model", "gpt-4o-mini")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", "You are a GxPrime FDA compliance expert. Analyze FDA 483 observations and provide actionable insights. Always address the organization as 'the company'.")
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.3)
                put("max_tokens", 2000)
            }
            
            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $API_KEY")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            android.util.Log.d("OpenAIService", "📡 FDA 483 analysis response code: ${response.code}")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val choices = jsonResponse.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val analysis = message.getString("content")
                    android.util.Log.d("OpenAIService", "📝 Received FDA 483 analysis: ${analysis.take(100)}...")
                    Result.success(analysis.trim())
                } else {
                    android.util.Log.e("OpenAIService", "❌ No choices in FDA 483 analysis response")
                    val error = Exception("No response from OpenAI")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "analyze_fda483")
                    com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "no_choices_in_response")
                    com.pramod.validator.utils.CrashReporting.logException(error, "OpenAI API returned no choices for FDA 483")
                    Result.failure(error)
                }
            } else {
                android.util.Log.e("OpenAIService", "❌ FDA 483 analysis API error: ${response.code} - ${responseBody}")
                val error = Exception("API Error: ${response.code} - ${responseBody ?: "No response body"}")
                com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "analyze_fda483")
                com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "api_error")
                com.pramod.validator.utils.CrashReporting.setCustomKey("http_code", response.code.toString())
                com.pramod.validator.utils.CrashReporting.logException(error, "OpenAI API error for FDA 483: ${response.code}")
                Result.failure(error)
            }
        } catch (e: Exception) {
            android.util.Log.e("OpenAIService", "❌ Exception during FDA 483 analysis: ${e.message}", e)
            com.pramod.validator.utils.CrashReporting.setCustomKey("operation", "analyze_fda483")
            com.pramod.validator.utils.CrashReporting.setCustomKey("error_type", "exception")
            com.pramod.validator.utils.CrashReporting.logException(e, "Exception during FDA 483 AI analysis")
            return Result.failure(e)
        }
    }
    
    private fun buildFda483Prompt(pdfText: String): String {
        // Limit PDF text to avoid token limits (first 8000 characters should be enough)
        val limitedText = pdfText.take(8000)
        
        return """
Analyze this FDA 483 document and provide a comprehensive analysis in JSON format.

Return a JSON with exactly these keys:
{
  "summary": string,  // Overall summary of the FDA 483 observations (2-3 sentences)
  "riskAreas": [      // List of identified risk areas
    {
      "area": string,           // Area name (e.g., "Documentation", "Quality Control", "Equipment")
      "description": string,    // Brief description of the risk area
      "specificDetails": string // Detailed explanation of what was flagged and why
    }
  ],
  "checklist": [      // Actionable checklist items to avoid getting flagged again
    {
      "item": string,     // Specific action item
      "priority": string  // "High", "Medium", or "Low"
    }
  ]
}

Constraints:
- Address as "the company" (not "your company")
- Be specific and actionable
- Focus on compliance improvements
- Use plain text only - no markdown, no asterisks, no emojis
- Provide 3-7 risk areas
- Provide 5-15 checklist items prioritized appropriately

FDA 483 Document Text:
$limitedText
        """.trimIndent()
    }
}

