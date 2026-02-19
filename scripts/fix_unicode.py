#!/usr/bin/env python3
"""
Comprehensive fix for QualityUnitQuestions.kt - escape all special characters
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace all special unicode characters that could break Kotlin strings
replacements = {
    '"': '"',  # Left double quotation mark
    '"': '"',  # Right double quotation mark
    ''': "'",  # Left single quotation mark
    ''': "'",  # Right single quotation mark
    '—': '-',  # Em dash
    '–': '-',  # En dash
    '→': '->',  # Rightwards arrow
    '←': '<-',  # Leftwards arrow
    '≤': '<=',  # Less-than or equal to
    '≥': '>=',  # Greater-than or equal to
    '×': 'x',  # Multiplication sign
    '÷': '/',  # Division sign
    '°': ' degrees',  # Degree sign
    '±': '+/-',  # Plus-minus sign
    '≠': '!=',  # Not equal to
    '≈': '~=',  # Almost equal to
    '…': '...',  # Horizontal ellipsis
}

for old, new in replacements.items():
    content = content.replace(old, new)

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed all special unicode characters")
