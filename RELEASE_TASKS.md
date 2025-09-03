# Release Tasks

When performing a release cycle, complete these tasks in order:

## Pre-Flight Check

- [ ] **Get Play Store versions (these are the source of truth):**
  - Play Store appVersionName: ___
  - Play Store mobile versionCode: ___
  
- [ ] **Calculate next versions:**
  - Next appVersionName: ___ (Play Store version + 0.01)
  - Next mobile versionCode: ___ (Play Store mobile + 2, must be odd)
  - Next TV versionCode: ___ (Next mobile + 1, must be even)

## Version Updates

- [ ] Update root build.gradle appVersionName (increment minor version)
- [ ] Update root build.gradle appVersionCode (to next mobile build number)  
- [ ] Update mobile/build.gradle versionCode (to next mobile build number)
- [ ] Update tv/build.gradle versionCode (to next TV build number)

## Changelog Creation

- [ ] Create changelog file for mobile (mobile_build_number.txt)
- [ ] Create changelog file for TV (tv_build_number.txt)

## Git Operations

- [ ] Add new changelog files to git
- [ ] Commit all changes with release message

## File Locations

- **Root build.gradle**: `build.gradle`
- **Mobile build.gradle**: `mobile/build.gradle` 
- **TV build.gradle**: `tv/build.gradle`
- **Changelogs**: `fastlane/metadata/android/en-US/changelogs/`

## Version Pattern

- **Version Name**: Increment minor version (X.Y → X.Y+1)
- **Mobile Build**: Always odd numbers (current_mobile + 2)
- **TV Build**: Always even numbers (mobile_build + 1)
- **Build Numbers**: Mobile and TV should be consecutive

## Current Example
- Version: 3.92 → 3.93
- Mobile: 1811 → 1813 (odd)
- TV: 1812 → 1814 (even)

## Changelog Format

```
vX.Y(Mobile)
* Feature or fix description
* Another change
```

```
vX.Y(TV)  
* Feature or fix description
* Another change
```