# 🌍 Android Translation Manager

Automated translation management for Android apps. Find missing translations, generate real translations, and apply them with proper XML escaping.

## ⚡ Quick Start

```bash
# From project root
./translate --all     # Find missing + translate + apply + fix escapes
```

## 🛠️ Commands

| Command | Description |
|---------|-------------|
| `./translate --find` | Scan for missing translations |
| `./translate --translate` | Generate real translations |
| `./translate --apply` | Apply to XML files |
| `./translate --all` | Complete workflow |
| `./translate --status` | Check current status |
| `./translate --clean` | Remove temp files |

## 📁 Project Structure

```
tools/translate/
├── find_missing_translations.py    # Scans for missing strings
├── generate_real_translations.py   # Contains real translations
├── apply_translations.py          # Updates XML files
└── fix_translation_escapes.py     # Fixes XML apostrophes
```

## 🔄 Workflow

### 1️⃣ Find Missing Translations
```bash
./translate --find
```
Scans all `values-*/strings.xml` files and compares with English base strings.

**Output:**
- `translation_batch.json` - Missing strings by language
- `translation_summary.txt` - Human-readable report

### 2️⃣ Generate Translations
```bash
./translate --translate
```
Uses `generate_real_translations.py` to create actual translations (no placeholders).

**Output:**
- `translations.json` - Real translated strings ready to apply

### 3️⃣ Apply & Fix
```bash
./translate --apply
```
Updates all XML files and automatically fixes apostrophe escaping.

## 🌐 Supported Languages

23 languages including:
`ar` `bn` `de` `el` `es` `fa` `fr` `hi` `in` `it` `ja` `ko` `pl` `pt` `pt-rBR` `ru` `sk` `tr` `uk` `ur` `vi` `zh` `zh-rTW`

## ➕ Adding New Translations

Edit `generate_real_translations.py`:

```python
"string_key": {
    "ar": "النص العربي",
    "fr": "Texte français",
    "es": "Texto español",
    "ja": "日本語テキスト"
}
```

## 🔗 API Integration

To use translation services, modify `generate_real_translations.py`:

```python
# Example with Google Translate
from googletrans import Translator

translator = Translator()
result = translator.translate(text, dest='fr')
```

## ⚠️ Important Notes

- ✅ Apostrophes are automatically escaped (`'` → `\'`)
- ✅ Build compatibility verified after each apply
- ✅ No placeholder translations (`[ar] Text`)
- ✅ Clean integration with Android resources

## 🧹 Cleanup

```bash
./translate --clean   # Remove all temp files
```

## 📝 Example Session

```bash
$ ./translate --all

🔍 Finding missing translations...
   Found 41 missing in Arabic
   Found 41 missing in French
   ...

🤖 Generating real translations...
   ✅ Updated 919 translations

✅ Applying to XML files...
   Updated 23 language files

🔧 Fixing XML escapes...
   Fixed apostrophes in 4 files

✅ Complete! Run build to verify.
```

---

Built for the Windscribe Android app • Clean translations without placeholders