#!/usr/bin/env python3

import json
import re
import sys
from pathlib import Path

STRING_NAME_RE = re.compile(r'<string\s+name="([^"]+)"')
INDENT_RE = re.compile(r'^([ \t]*)<string\s', re.M)
# Anything already written as an entity is left alone; apostrophes belong to fix_translation_escapes.py.
BARE_AMPERSAND_RE = re.compile(r'&(?!(?:amp|lt|gt|quot|apos|#\d+|#x[0-9a-fA-F]+);)')

def escape_xml(text):
    """Escape the characters that would break XML."""
    if not text:
        return ""
    text = BARE_AMPERSAND_RE.sub('&amp;', text)
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    return text

def add_translations_to_file(file_path, translations_dict):
    """Insert missing translations before </resources>, leaving the rest of the file untouched."""
    try:
        content = Path(file_path).read_text(encoding='utf-8')

        existing_keys = set(STRING_NAME_RE.findall(content))
        new_strings = {
            key: value
            for key, value in sorted(translations_dict.items())
            if key not in existing_keys
        }

        if not new_strings:
            print(f"  \u2139\ufe0f  No new translations needed for {Path(file_path).name}")
            return 0

        closing = content.rfind('</resources>')
        if closing == -1:
            print(f"  \u274c No </resources> in {Path(file_path).name}, skipping")
            return 0

        indent_match = INDENT_RE.search(content)
        indent = indent_match.group(1) if indent_match else '    '

        block = "".join(
            f'{indent}<string name="{key}">{escape_xml(value)}</string>\n'
            for key, value in new_strings.items()
        )
        if not content[:closing].endswith('\n'):
            block = '\n' + block

        Path(file_path).write_text(content[:closing] + block + content[closing:], encoding='utf-8')

        print(f"  \u2705 Added {len(new_strings)} translations to {Path(file_path).name}")
        return len(new_strings)

    except Exception as e:
        print(f"  \u274c Error updating {file_path}: {e}")
        return 0

def apply_translations_from_json(translations_file='translations.json'):
    """Apply translations from a JSON file to the respective language files."""

    script_dir = Path(__file__).parent
    base_dir = script_dir.parent.parent  # Go up from tools/translate to project root
    res_dir = base_dir / 'base/src/main/res'

    translations_path = script_dir / translations_file

    if not translations_path.exists():
        print(f"❌ Translations file not found: {translations_path}")
        print("   Please create translations.json with machine translations first.")
        return False

    # Load translations
    with open(translations_path, 'r', encoding='utf-8') as f:
        translations_data = json.load(f)

    print("=" * 60)
    print("APPLYING TRANSLATIONS")
    print("=" * 60)

    total_added = 0
    languages_updated = 0

    # Apply translations for each language
    for lang_code, lang_data in translations_data.items():
        folder = lang_data.get('folder')
        if not folder:
            print(f"\n⚠️  No folder specified for {lang_code}, skipping...")
            continue

        strings_file = res_dir / folder / 'strings.xml'

        if not strings_file.exists():
            print(f"\n⚠️  File not found: {strings_file}, skipping...")
            continue

        print(f"\nProcessing {lang_data.get('language_name', lang_code)} ({folder}):")
        print("-" * 40)

        translations = lang_data.get('translations', {})
        if translations:
            added = add_translations_to_file(strings_file, translations)
            total_added += added
            if added > 0:
                languages_updated += 1
        else:
            print(f"  ⚠️  No translations found for {lang_code}")

    print("\n" + "=" * 60)
    print("SUMMARY")
    print("=" * 60)
    print(f"✅ Updated {languages_updated} language files")
    print(f"✅ Added {total_added} total translations")

    return True

def main():
    # Check if translations file exists
    translations_file = 'translations.json'

    if len(sys.argv) > 1:
        translations_file = sys.argv[1]

    success = apply_translations_from_json(translations_file)

    if not success:
        print("\n" + "=" * 60)
        print("HOW TO USE THIS SCRIPT:")
        print("=" * 60)
        print("1. First run: python3 find_missing_translations.py")
        print("2. Get the translation_batch.json file translated")
        print("3. Save translations as translations.json with this structure:")
        print("""
{
  "es": {
    "folder": "values-es",
    "language_name": "Spanish",
    "translations": {
      "string_key": "translated value",
      ...
    }
  },
  ...
}
        """)
        print("4. Run: python3 apply_translations.py translations.json")
        sys.exit(1)

if __name__ == "__main__":
    main()