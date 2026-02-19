#!/usr/bin/env python3
"""
Fix syntax errors in QualityUnitQuestions.kt by converting em dashes to regular hyphens
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Replace em dashes with regular hyphens in Question strings
# Em dash: — (U+2014)
# En dash: – (U+2013)
content = content.replace('—', '-')
content = content.replace('–', '-')

# Also replace any curly quotes with straight quotes
content = content.replace('"', '"')
content = content.replace('"', '"')
content = content.replace(''', "'")
content = content.replace(''', "'")

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed syntax errors by replacing special characters")
