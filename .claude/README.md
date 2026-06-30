# 🤖 Claude Skills for Android App

Custom skills and commands for Claude to assist with Android app development.

## 🌍 Translation Manager

Automated translation management system with real translations (no placeholders).

### ⚡ Quick Commands

```bash
./translate           # Interactive menu
./translate --all     # Complete workflow (find + translate + apply + fix)
./translate --find    # Scan for missing translations
./translate --apply   # Apply translations with auto-escape fix
./translate --status  # Check current status
./translate --clean   # Remove temp files
```

### ✨ Features

- **Real Translations** - No `[ar] Text` placeholders
- **Auto XML Escape** - Apostrophes automatically escaped
- **Build Safe** - Verified Android compatibility
- **23 Languages** - Full multi-language support

### 📁 Output Files

| File | Purpose |
|------|---------|
| `translation_batch.json` | Missing strings by language |
| `translations.json` | Real translations to apply |
| `translation_summary.txt` | Human-readable report |

### 🔄 Workflow Example

```bash
$ ./translate --all

🔍 Finding missing translations...
   ✓ Found 919 missing strings across 23 languages

🤖 Generating real translations...
   ✓ Applied actual translations (no placeholders)

✅ Applying to XML files...
   ✓ Updated 23 language files

🔧 Fixing XML escapes...
   ✓ Fixed apostrophes in 4 files

✅ Complete! Ready to build.
```

### 🛠️ Claude Commands

Use these slash commands in Claude:
- `/translate` - Launch translation manager
- `/healthcheck` - Run full verification pipeline

### ⚙️ Configuration

The translation system is located in `tools/translate/` with:
- `find_missing_translations.py` - Scanner
- `generate_real_translations.py` - Real translations
- `apply_translations.py` - XML updater
- `fix_translation_escapes.py` - Apostrophe fixer

### 📝 Notes

- Translations integrate cleanly (no comment markers)
- Always run build after applying translations
- Extend `generate_real_translations.py` for new strings
- Compatible with Google Translate, DeepL, OpenAI APIs

---

Built for Windscribe Android • Clean, production-ready translations