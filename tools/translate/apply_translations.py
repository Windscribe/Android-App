#!/usr/bin/env python3

import xml.etree.ElementTree as ET
import os
import json
import sys
from pathlib import Path
import html

def escape_xml(text):
    """Properly escape text for XML."""
    if not text:
        return ""
    # Escape basic XML entities
    text = text.replace('&', '&amp;')
    text = text.replace('<', '&lt;')
    text = text.replace('>', '&gt;')
    text = text.replace('"', '&quot;')
    text = text.replace("'", '&apos;')
    return text

def add_translations_to_file(file_path, translations_dict):
    """Add missing translations to a strings.xml file."""
    try:
        # Parse the XML file
        tree = ET.parse(file_path)
        root = tree.getroot()

        # Get existing keys to avoid duplicates
        existing_keys = set()
        for string_elem in root.findall('string'):
            key = string_elem.get('name')
            if key:
                existing_keys.add(key)

        # Count additions
        added_count = 0

        # Find the position before </resources>
        # We'll add new strings at the end, before the closing tag

        # Add each missing translation
        for key, value in sorted(translations_dict.items()):
            if key not in existing_keys:
                # Create new string element
                new_elem = ET.Element('string')
                new_elem.set('name', key)
                new_elem.text = value
                root.append(new_elem)
                added_count += 1

        if added_count > 0:
            # Format and write the XML
            ET.indent(tree, space="    ")

            # Write to file with XML declaration
            tree.write(file_path, encoding='utf-8', xml_declaration=True)

            # Fix the file to have proper formatting
            with open(file_path, 'r', encoding='utf-8') as f:
                content = f.read()

            # Fix XML declaration
            content = content.replace("<?xml version='1.0' encoding='utf-8'?>",
                                    '<?xml version="1.0" encoding="utf-8"?>')

            # Ensure proper spacing
            content = content.replace('</resources>', '\n</resources>')

            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)

            print(f"  ✅ Added {added_count} translations to {file_path.name}")
        else:
            print(f"  ℹ️  No new translations needed for {file_path.name}")

        return added_count

    except Exception as e:
        print(f"  ❌ Error updating {file_path}: {e}")
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