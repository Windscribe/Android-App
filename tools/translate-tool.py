#!/usr/bin/env python3
"""
Android App Translation Manager
Interactive tool for managing app translations
"""

import os
import sys
import subprocess
from pathlib import Path
import json

class TranslationManager:
    def __init__(self):
        self.script_dir = Path(__file__).parent
        self.translate_dir = self.script_dir / 'translate'
        self.base_dir = self.script_dir.parent

    def print_header(self):
        """Print the tool header"""
        print("\n" + "="*60)
        print("🌍 ANDROID APP TRANSLATION MANAGER")
        print("="*60)
        print()

    def print_menu(self):
        """Display the main menu"""
        print("Choose an action:")
        print()
        print("1. 🔍 Find missing translations")
        print("2. 🤖 Generate machine translations")
        print("3. ✅ Apply translations to XML files")
        print("4. 🚀 Run complete workflow (find → translate → apply)")
        print("5. 📊 Show translation status")
        print("6. 🧹 Clean temporary files")
        print("7. ❌ Exit")
        print()

    def run_command(self, cmd, cwd=None):
        """Run a shell command and return the result"""
        try:
            result = subprocess.run(
                cmd,
                shell=True,
                capture_output=True,
                text=True,
                cwd=cwd or self.translate_dir
            )
            print(result.stdout)
            if result.stderr:
                print(result.stderr, file=sys.stderr)
            return result.returncode == 0
        except Exception as e:
            print(f"❌ Error running command: {e}")
            return False

    def find_missing(self):
        """Find missing translations"""
        print("\n🔍 Finding missing translations...")
        print("-"*40)
        return self.run_command("python3 find_missing_translations.py")

    def generate_translations(self):
        """Generate machine translations"""
        batch_file = self.translate_dir / 'translation_batch.json'

        if not batch_file.exists():
            print("⚠️  No translation batch found. Running find missing first...")
            if not self.find_missing():
                return False

        print("\n🤖 Generating machine translations...")
        print("-"*40)

        # First generate the template
        if not self.run_command("python3 generate_translations.py"):
            return False

        # Now do actual machine translation
        print("\n📝 Getting machine translations...")
        return self.machine_translate()

    def machine_translate(self):
        """Perform machine translation of missing strings"""
        batch_file = self.translate_dir / 'translation_batch.json'

        if not batch_file.exists():
            print("❌ Translation batch file not found!")
            return False

        # Check if generate_real_translations.py exists
        real_translations_script = self.translate_dir / 'generate_real_translations.py'
        if real_translations_script.exists():
            print("\n🤖 Generating real translations...")
            return self.run_command("python3 generate_real_translations.py")
        else:
            print("\n⚠️  Real translation script not found!")
            print("   To add real translations:")
            print("   1. Create a translation script with actual translations")
            print("   2. Or integrate with a translation API (Google Translate, DeepL, etc.)")
            print("\n   For now, you can manually add translations to the translation_batch.json")
            return False

    def apply_translations(self):
        """Apply translations to XML files"""
        translations_file = self.translate_dir / 'translations.json'

        if not translations_file.exists():
            print("⚠️  No translations found. Generate translations first.")
            return False

        print("\n✅ Applying translations to XML files...")
        print("-"*40)

        # Apply translations
        if not self.run_command("python3 apply_translations.py"):
            return False

        # Fix any XML escape issues (apostrophes, etc.)
        escape_fix_script = self.translate_dir / 'fix_translation_escapes.py'
        if escape_fix_script.exists():
            print("\n🔧 Fixing XML escape issues...")
            return self.run_command("python3 fix_translation_escapes.py")

        return True

    def show_status(self):
        """Show current translation status"""
        print("\n📊 Translation Status")
        print("-"*40)

        # Check for existing files
        files_to_check = [
            ('translation_batch.json', '📦 Translation batch'),
            ('translation_summary.txt', '📄 Summary'),
            ('translations.json', '🌍 Translations'),
            ('translation_request.txt', '📝 Request')
        ]

        for filename, label in files_to_check:
            file_path = self.translate_dir / filename
            if file_path.exists():
                size = file_path.stat().st_size
                print(f"✅ {label}: {filename} ({size:,} bytes)")
            else:
                print(f"❌ {label}: Not found")

        # Show summary if available
        summary_file = self.translate_dir / 'translation_summary.txt'
        if summary_file.exists():
            print("\n📋 Summary:")
            print("-"*40)
            with open(summary_file, 'r') as f:
                all_lines = f.readlines()
                lines = all_lines[:20]  # Show first 20 lines
                for line in lines:
                    print(line.rstrip())
                if len(all_lines) > 20:
                    print("... (truncated)")

    def clean_files(self):
        """Clean temporary translation files"""
        print("\n🧹 Cleaning temporary files...")
        print("-"*40)

        files_to_remove = [
            'translation_batch.json',
            'translation_summary.txt',
            'translations.json',
            'translation_request.txt'
        ]

        removed = 0
        for filename in files_to_remove:
            file_path = self.translate_dir / filename
            if file_path.exists():
                file_path.unlink()
                print(f"  ✅ Removed: {filename}")
                removed += 1

        if removed > 0:
            print(f"\n✅ Cleaned {removed} files")
        else:
            print("ℹ️  No files to clean")

    def run_full_workflow(self):
        """Run the complete translation workflow"""
        print("\n🚀 Running complete translation workflow")
        print("="*40)

        # Step 1: Find missing
        print("\nStep 1/3: Finding missing translations...")
        if not self.find_missing():
            print("❌ Failed to find missing translations")
            return False

        # Check if there are missing translations
        batch_file = self.translate_dir / 'translation_batch.json'
        if not batch_file.exists():
            print("✅ No missing translations found! All languages are complete.")
            return True

        # Step 2: Generate translations
        print("\nStep 2/3: Generating translations...")
        if not self.generate_translations():
            print("❌ Failed to generate translations")
            return False

        # Step 3: Apply translations
        print("\nStep 3/3: Applying translations...")
        if not self.apply_translations():
            print("❌ Failed to apply translations")
            return False

        print("\n✅ Workflow completed successfully!")
        print("   Review changes with: git diff")
        return True

    def run(self):
        """Main interactive loop"""
        self.print_header()

        while True:
            self.print_menu()

            try:
                choice = input("Enter your choice (1-7): ").strip()

                if choice == '1':
                    self.find_missing()
                elif choice == '2':
                    self.generate_translations()
                elif choice == '3':
                    self.apply_translations()
                elif choice == '4':
                    self.run_full_workflow()
                elif choice == '5':
                    self.show_status()
                elif choice == '6':
                    self.clean_files()
                elif choice == '7':
                    print("\n👋 Goodbye!")
                    break
                else:
                    print("⚠️  Invalid choice. Please try again.")

                print()  # Add spacing

            except KeyboardInterrupt:
                print("\n\n👋 Goodbye!")
                break
            except Exception as e:
                print(f"\n❌ Error: {e}")

def main():
    """Entry point"""
    import argparse

    parser = argparse.ArgumentParser(description='Android App Translation Manager')
    parser.add_argument('--find', action='store_true', help='Find missing translations')
    parser.add_argument('--translate', action='store_true', help='Generate machine translations')
    parser.add_argument('--apply', action='store_true', help='Apply translations to XML files')
    parser.add_argument('--status', action='store_true', help='Show translation status')
    parser.add_argument('--clean', action='store_true', help='Clean temporary files')
    parser.add_argument('--all', action='store_true', help='Run complete workflow')

    args = parser.parse_args()

    manager = TranslationManager()

    # Non-interactive mode if arguments provided
    if any(vars(args).values()):
        if args.find:
            manager.find_missing()
        elif args.translate:
            manager.generate_translations()
        elif args.apply:
            manager.apply_translations()
        elif args.status:
            manager.show_status()
        elif args.clean:
            manager.clean_files()
        elif args.all:
            manager.run_full_workflow()
    else:
        # Interactive mode
        manager.run()

if __name__ == "__main__":
    main()