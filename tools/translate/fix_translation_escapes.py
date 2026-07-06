#!/usr/bin/env python3
"""
Fix XML escape issues in translation strings
"""
import re
from pathlib import Path

def fix_apostrophes_in_file(file_path):
    """Fix unescaped apostrophes in XML string resources"""

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Find all string resources
    string_pattern = r'(<string[^>]*>)(.*?)(</string>)'

    def fix_string(match):
        start = match.group(1)
        text = match.group(2)
        end = match.group(3)

        # Don't process CDATA sections
        if '<![CDATA[' in text:
            return match.group(0)

        # Fix unescaped apostrophes (but not already escaped ones)
        # Replace ' with \' but not if it's already \'
        fixed_text = re.sub(r"(?<!\\)'", r"\'", text)

        return start + fixed_text + end

    # Apply fixes
    fixed_content = re.sub(string_pattern, fix_string, content, flags=re.DOTALL)

    # Save back
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(fixed_content)

    return content != fixed_content  # Return True if changes were made

def main():
    """Fix all translation files"""
    base_dir = Path(__file__).parent.parent.parent
    values_dir = base_dir / 'base' / 'src' / 'main' / 'res'

    # Languages with reported issues
    problem_langs = ['fr', 'it', 'uk', 'tr']

    print("🔧 Fixing XML escape issues in translation files...")
    print("=" * 50)

    fixed_count = 0

    for lang in problem_langs:
        lang_dir = values_dir / f'values-{lang}'
        strings_file = lang_dir / 'strings.xml'

        if strings_file.exists():
            print(f"\n📝 Checking {lang}...")
            if fix_apostrophes_in_file(strings_file):
                print(f"  ✅ Fixed apostrophes in values-{lang}/strings.xml")
                fixed_count += 1
            else:
                print(f"  ℹ️  No changes needed for values-{lang}/strings.xml")

    # Also check for incomplete strings (Turkish issue)
    print("\n🔍 Checking for incomplete strings...")
    tr_file = values_dir / 'values-tr' / 'strings.xml'
    if tr_file.exists():
        with open(tr_file, 'r', encoding='utf-8') as f:
            content = f.read()

        # Fix the incomplete encryption_unavailable_warning
        if 'encryption_unavailable_warning">Cihazınız' in content and 'Maksimum güvenlik için' not in content:
            # The string is incomplete, let's fix it
            old_incomplete = 'encryption_unavailable_warning">Cihazınız şifrelenmiş kimlik bilgisi depolamayı desteklemiyor. Oturum belirteçleri ve VPN anahtarları azaltılmış güvenlikle saklanacak.'
            new_complete = 'encryption_unavailable_warning">Cihazınız şifrelenmiş kimlik bilgisi depolamayı desteklemiyor. Oturum belirteçleri ve VPN anahtarları azaltılmış güvenlikle saklanacak.\n\nMaksimum güvenlik için, güvenilmeyen ağlarda veya root\'lanmışsa bu cihazı kullanmaktan kaçının.</string>'

            if old_incomplete in content:
                content = content.replace(old_incomplete, new_complete)
                with open(tr_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                print("  ✅ Fixed incomplete string in Turkish translation")
                fixed_count += 1

    print(f"\n✅ Fixed {fixed_count} files")
    print("\nNext step: Run the build again to verify fixes")

if __name__ == "__main__":
    main()