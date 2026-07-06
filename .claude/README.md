# 🤖 Claude Commands & Skills

Custom commands for Claude in the Windscribe Android project.

## 📋 Available Commands

| Command | Description |
|---------|------------|
| `/translate` | Launch translation manager |
| `/healthcheck` | Run verification pipeline (lint, test, build) |

## 🛠️ Command Details

### `/translate`
Manages app translations - finds missing strings and applies real translations.
- 📖 Full docs: [`tools/translate/README.md`](../tools/translate/README.md)
- ⚡ Quick use: `/translate` or `./translate --all`

### `/healthcheck`
Runs full local verification before pushing code.
- ✅ ktlint auto-format
- ✅ All unit tests
- ✅ FDroid + Google debug builds

## 📁 Directory Structure

```
.claude/
├── commands/       # Command definitions
│   ├── translate.md
│   └── healthcheck.md
└── skills/         # Skill configurations
    ├── translate.json
    └── healthcheck.json
```

## 🚀 Quick Start

Just type `/` in Claude to see available commands or use them directly:

```bash
/translate        # Interactive translation menu
/healthcheck      # Full verification pipeline
```

---

For detailed tool documentation, see the respective README files in each tool directory.