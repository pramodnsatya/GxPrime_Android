package com.pramod.validator.ui.components

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Composable for date picker input field
 * Opens Android DatePickerDialog when clicked
 */
@Composable
fun DatePickerField(
    selectedDate: Long?,
    onDateSelected: (Long) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    val calendar = Calendar.getInstance()
    if (selectedDate != null && selectedDate > 0) {
        calendar.timeInMillis = selectedDate
    }
    
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    
    val datePickerDialog = DatePickerDialog(
        context,
        { _, selectedYear, selectedMonth, selectedDay ->
            val newCalendar = Calendar.getInstance()
            newCalendar.set(selectedYear, selectedMonth, selectedDay)
            onDateSelected(newCalendar.timeInMillis)
        },
        year,
        month,
        day
    )
    
    // Set minimum date to today
    datePickerDialog.datePicker.minDate = System.currentTimeMillis()
    
    OutlinedTextField(
        value = if (selectedDate != null && selectedDate > 0) {
            dateFormat.format(Date(selectedDate))
        } else {
            ""
        },
        onValueChange = { },
        label = { Text(label) },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) {
                if (enabled) {
                    showDatePicker = true
                }
            },
        enabled = false,
        readOnly = true,
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select date",
                tint = if (enabled) Color(0xFF64748B) else Color(0xFFCBD5E1)
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1E3A8A), // blue-900
            unfocusedBorderColor = Color(0xFFE2E8F0), // slate-200
            disabledBorderColor = Color(0xFFE2E8F0),
            disabledTextColor = Color(0xFF0F172A), // slate-900
            disabledLabelColor = Color(0xFF64748B) // slate-500
        )
    )
    
    if (showDatePicker) {
        LaunchedEffect(Unit) {
            datePickerDialog.show()
            showDatePicker = false
        }
    }
}

