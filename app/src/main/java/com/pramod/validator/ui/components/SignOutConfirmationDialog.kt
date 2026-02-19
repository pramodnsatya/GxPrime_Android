package com.pramod.validator.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignOutConfirmationDialog(
    isVisible: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Color.White,
            title = { 
                Text(
                    "Confirm Sign Out",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A) // slate-900
                ) 
            },
            text = { 
                Text(
                    "Are you sure you want to sign out?",
                    fontSize = 15.sp,
                    color = Color(0xFF64748B) // slate-500
                ) 
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A) // blue-900
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Sign Out",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF0F172A) // slate-900
                    )
                ) {
                    Text(
                        "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

