#!/usr/bin/env python3
"""
Comprehensive fix for all duplicate text in QualityUnitQuestions.kt
"""

import re

# Read the file
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'r', encoding='utf-8') as f:
    lines = f.readlines()

fixed_lines = []
for i, line in enumerate(lines):
    # Check if this is a Question line with potential duplicate
    if 'Question("' in line and '), ' in line:
        # Pattern: ends with number), extra text", number),
        # We want to keep only the first occurrence
        match = re.match(r'(\s*Question\("[^"]+", "[^"]+", ")', line)
        if match:
            # Find all occurrences of "), "
            parts = line.split('"), ')
            if len(parts) > 2:
                # Multiple closing patterns found - this is a duplicate
                # Reconstruct: keep first part + first closing
                # Find the question number
                num_match = re.search(r', (\d+)\),', parts[1])
                if num_match:
                    num = num_match.group(1)
                    # Reconstruct the line properly
                    # Extract the question text (everything between third and fourth quote)
                    text_match = re.match(r'\s*Question\("([^"]+)", "([^"]+)", "([^"]+)"', line)
                    if text_match:
                        qid, subdomain, text = text_match.groups()
                        # Take only the first occurrence of the text (before first number),)
                        text_parts = text.split(f'", {num}),')
                        if len(text_parts) > 1:
                            clean_text = text_parts[0]
                            fixed_line = f'            Question("{qid}", "{subdomain}", "{clean_text}", {num}),\n'
                            fixed_lines.append(fixed_line)
                            print(f"Fixed line {i+1}: {qid}")
                            continue
    
    # If not fixed above, keep original
    fixed_lines.append(line)

# Write back
with open('app/src/main/java/com/example/validator/data/QualityUnitQuestions.kt', 'w', encoding='utf-8') as f:
    f.writelines(fixed_lines)

print("Fixed all duplicate entries")
