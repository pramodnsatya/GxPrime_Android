#!/usr/bin/env python3
"""
Fix all duplicate text in Question entries caused by bad regex replacements
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Find all Question entries and fix duplicates
# Pattern: Question("id", "subdomain", "text", number), extra_duplicate_text", number),
pattern = r'Question\("([^"]+)", "([^"]+)", "([^"]+)", (\d+)\), [^,\n]+", \4\),'

def fix_question(match):
    q_id, subdomain, text, num = match.groups()
    # Return the correct format without duplication
    return f'Question("{q_id}", "{subdomain}", "{text}", {num}),'

# Apply the fix
content = re.sub(pattern, fix_question, content)

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Fixed duplicate text in Question entries")
