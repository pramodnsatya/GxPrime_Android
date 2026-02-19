package com.pramod.validator.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

object PdfTextExtractor {
    suspend fun extractTextFromUri(context: Context, uri: Uri): Result<String> {
        return try {
            Log.d("PdfTextExtractor", "Opening PDF from URI: $uri")
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("PdfTextExtractor", "Failed to open input stream from URI")
                return Result.failure(Exception("Could not open file. Please check file permissions."))
            }
            
            try {
                extractTextFromInputStream(inputStream)
            } finally {
                try {
                    inputStream.close()
                } catch (e: Exception) {
                    Log.w("PdfTextExtractor", "Error closing input stream: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "Error extracting text from PDF: ${e.message}", e)
            Log.e("PdfTextExtractor", "Stack trace: ${e.stackTraceToString()}")
            Result.failure(Exception("Failed to extract PDF text: ${e.message}", e))
        } catch (e: Throwable) {
            Log.e("PdfTextExtractor", "Fatal error extracting text from PDF: ${e.message}", e)
            Result.failure(Exception("Fatal error extracting PDF: ${e.message}", e))
        }
    }
    
    private suspend fun extractTextFromInputStream(inputStream: InputStream): Result<String> {
        var document: PDDocument? = null
        return try {
            Log.d("PdfTextExtractor", "Loading PDF document...")
            document = PDDocument.load(inputStream)
            
            if (document == null) {
                Log.e("PdfTextExtractor", "Failed to load PDF document")
                return Result.failure(Exception("Failed to load PDF document"))
            }
            
            Log.d("PdfTextExtractor", "PDF document loaded. Number of pages: ${document.numberOfPages}")
            
            val stripper = PDFTextStripper()
            Log.d("PdfTextExtractor", "Extracting text from PDF...")
            val text = stripper.getText(document)
            
            Log.d("PdfTextExtractor", "Extracted ${text.length} characters from PDF")
            
            if (text.isBlank()) {
                Log.w("PdfTextExtractor", "PDF text extraction returned blank text")
                Result.failure(Exception("PDF appears to be empty or could not be read. The PDF might be image-based or encrypted."))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Log.e("PdfTextExtractor", "Error extracting text from PDF stream: ${e.message}", e)
            Log.e("PdfTextExtractor", "Stack trace: ${e.stackTraceToString()}")
            Result.failure(Exception("Failed to extract text from PDF: ${e.message}", e))
        } catch (e: Throwable) {
            Log.e("PdfTextExtractor", "Fatal error extracting text from PDF: ${e.message}", e)
            Result.failure(Exception("Fatal error: ${e.message}", e))
        } finally {
            try {
                document?.close()
                Log.d("PdfTextExtractor", "PDF document closed")
            } catch (e: Exception) {
                Log.e("PdfTextExtractor", "Error closing PDF document: ${e.message}", e)
            }
        }
    }
}


