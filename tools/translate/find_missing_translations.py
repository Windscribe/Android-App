#!/usr/bin/env python3

import xml.etree.ElementTree as ET
import os
import json
import sys
from pathlib import Path

def extract_strings_from_xml(file_path):
    """Extract all string keys and values from a strings.xml file."""
    strings = {}
    try:
        tree = ET.parse(file_path)
        root = tree.getroot()

        for string_elem in root.findall('string'):
            key = string_elem.get('name')
            value = string_elem.text if string_elem.text else ""
            if key:
                strings[key] = value
    except Exception as e:
        print(f"Error parsing {file_path}: {e}")

    return strings

def get_language_from_folder(folder_name):
    """Extract language code from folder name like 'values-es' -> 'es'."""
    if folder_name == 'values':
        return 'en'
    return folder_name.replace('values-', '')

def find_missing_translations():
    """Find all missing translations for each language."""

    # Base paths - dynamically find project root
    script_dir = Path(__file__).parent
    base_dir = script_dir.parent.parent  # Go up from tools/translate to project root
    res_dir = base_dir / 'base/src/main/res'

    # Extract English strings (source of truth)
    english_file = res_dir / 'values/strings.xml'
    english_strings = extract_strings_from_xml(english_file)

    print(f"Found {len(english_strings)} strings in English file")

    # Find all language folders
    language_folders = []
    for folder in res_dir.iterdir():
        if folder.is_dir() and folder.name.startswith('values-'):
            # Skip ldrtl (layout direction) folder
            if 'ldrtl' not in folder.name:
                strings_file = folder / 'strings.xml'
                if strings_file.exists():
                    language_folders.append(folder.name)

    language_folders.sort()
    print(f"\nFound {len(language_folders)} language folders:")
    for folder in language_folders:
        print(f"  - {folder}")

    # Find missing translations for each language
    missing_translations = {}

    for folder in language_folders:
        lang_code = get_language_from_folder(folder)
        lang_file = res_dir / folder / 'strings.xml'
        lang_strings = extract_strings_from_xml(lang_file)

        # Find missing keys
        missing_keys = set(english_strings.keys()) - set(lang_strings.keys())

        if missing_keys:
            missing_translations[lang_code] = {
                'folder': folder,
                'missing_count': len(missing_keys),
                'missing_strings': {}
            }

            for key in sorted(missing_keys):
                missing_translations[lang_code]['missing_strings'][key] = english_strings[key]

            print(f"\n{folder}: Missing {len(missing_keys)} translations")
            # Show first 5 missing keys as examples
            example_keys = list(sorted(missing_keys))[:5]
            for key in example_keys:
                print(f"  - {key}")
            if len(missing_keys) > 5:
                print(f"  ... and {len(missing_keys) - 5} more")

    return missing_translations, english_strings

def create_translation_batch(missing_translations):
    """Create a batch structure for translation."""

    # Language code mapping for better translations
    language_names = {
        'ar': 'Arabic',
        'bn': 'Bengali',
        'de': 'German',
        'el': 'Greek',
        'es': 'Spanish',
        'fa': 'Persian/Farsi',
        'fr': 'French',
        'hi': 'Hindi',
        'in': 'Indonesian',
        'it': 'Italian',
        'ja': 'Japanese',
        'ko': 'Korean',
        'pl': 'Polish',
        'pt': 'Portuguese',
        'pt-rBR': 'Brazilian Portuguese',
        'ru': 'Russian',
        'sk': 'Slovak',
        'tr': 'Turkish',
        'uk': 'Ukrainian',
        'ur': 'Urdu',
        'vi': 'Vietnamese',
        'zh': 'Chinese Simplified',
        'zh-rTW': 'Chinese Traditional'
    }

    # Create batch for translation
    batch = {
        'languages': {},
        'summary': {
            'total_languages': len(missing_translations),
            'total_missing': sum(data['missing_count'] for data in missing_translations.values())
        }
    }

    for lang_code, data in missing_translations.items():
        lang_name = language_names.get(lang_code, lang_code)
        batch['languages'][lang_code] = {
            'language_name': lang_name,
            'folder': data['folder'],
            'missing_count': data['missing_count'],
            'strings_to_translate': data['missing_strings']
        }

    return batch

def save_translation_batch(batch, output_file='translation_batch.json'):
    """Save the translation batch to a JSON file."""
    script_dir = Path(__file__).parent
    output_path = script_dir / output_file

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(batch, f, ensure_ascii=False, indent=2)

    print(f"\n✅ Translation batch saved to: {output_path}")
    print(f"   Total languages needing translation: {batch['summary']['total_languages']}")
    print(f"   Total missing strings: {batch['summary']['total_missing']}")

    return output_path

def main():
    print("=" * 60)
    print("FINDING MISSING TRANSLATIONS")
    print("=" * 60)

    # Find missing translations
    missing_translations, english_strings = find_missing_translations()

    if not missing_translations:
        print("\n✅ All languages have complete translations!")
        return

    # Create translation batch
    print("\n" + "=" * 60)
    print("CREATING TRANSLATION BATCH")
    print("=" * 60)

    batch = create_translation_batch(missing_translations)

    # Save batch to JSON file
    batch_file = save_translation_batch(batch)

    print("\n" + "=" * 60)
    print("NEXT STEPS:")
    print("=" * 60)
    print("1. The translation batch has been saved to translation_batch.json")
    print("2. This file contains all missing strings organized by language")
    print("3. Run apply_translations.py after getting machine translations")

    # Also create a summary file for easy viewing
    script_dir = Path(__file__).parent
    summary_file = script_dir / 'translation_summary.txt'
    with open(summary_file, 'w') as f:
        f.write("MISSING TRANSLATIONS SUMMARY\n")
        f.write("=" * 60 + "\n\n")

        for lang_code, data in batch['languages'].items():
            f.write(f"\n{data['language_name']} ({data['folder']}): {data['missing_count']} missing\n")
            f.write("-" * 40 + "\n")

            # Show first 10 strings as examples
            count = 0
            for key, value in data['strings_to_translate'].items():
                if count < 10:
                    f.write(f"  {key}: {value[:50]}{'...' if len(value) > 50 else ''}\n")
                    count += 1
                else:
                    f.write(f"  ... and {data['missing_count'] - 10} more\n")
                    break

    print(f"\nSummary saved to: {summary_file}")

if __name__ == "__main__":
    main()