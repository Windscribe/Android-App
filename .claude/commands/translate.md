# Translation Manager

Run the Android app translation management tools to find missing translations, generate templates, and apply translations.

## Available Actions:

### 1. Find Missing Translations
Scan all language files and identify which strings are missing translations.

### 2. Generate Machine Translations
Create machine translations for all missing strings using AI.

### 3. Apply Translations
Apply the translated strings back to the XML files.

### 4. Full Process
Run the complete translation workflow: find → translate → apply.

## How to use:
- Choose an action from the menu
- The tool will guide you through the process
- Review the changes before committing

## Files created:
- `translation_batch.json` - All missing strings by language
- `translation_summary.txt` - Human-readable summary
- `translations.json` - Machine translations to apply

The scripts are located in `tools/translate/`