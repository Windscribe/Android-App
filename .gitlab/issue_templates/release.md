# Release Process for iOS App

This document outlines the steps to follow when releasing the Android app, including creating a release branch, internal and Open Beta distribution, QA testing, and final Play store submission.

- [ ] Feature Freeze Discussion
 - **Timing:** Prior to the release cycle, schedule a feature freeze discussion with the team.
 - **Objective:** Review all pending features and ensure only necessary changes are pushed to the release branch. Bug fixes are still allowed 
 post-freeze.

- [ ] Create Release Branch
 - **Branching:** Create a release branch from the main branch.
  ```bash
  git checkout -b release-branch-x.x.x
  ```
- [ ] Test all issues locally as much as possible.
- [ ] Send release to internal Qa on Testflight and also change version info in gitlab ENV variables.
- [ ] Fix Qa feedback, update release and repeat until full Qa pass is received. 
- [ ] Prepare Open beta changelog and update.
- [ ] Release app to Open beta.
- [ ] Wait for regressions , major issues and provide hotfixes.
- [ ] Fix Users feedback, update release and repeat.
- [ ] Prepare final changelog and update in fastlane/metadata/android/en-US/changelogs
- [ ] Send app for play store review.
- [ ] Start stagged rollout with 2% Users and keep increasing it every other day.
- [ ] Merge release branch in to main
- [ ] Create tag from main branch
 ```bash
  git tag -a vX.X.X -m "Release X.X.X"
  git push origin vX.X.X
```
- [ ] Sync code to github and create a tagged release.
- [ ] Create admin panel release for website(android, AndroidTV, FireTV) and verify changes on https://www-staging.windscribe.com/
- [ ] Create admin panel release for android-direct apk and android-direct-tv-apk.
- [ ] Create Fdroid MR and follow up on release to Fdroid.
- [ ] Create Amazon app store release.
- [ ] Create Huawei store release.
- [ ] For hotfix Create new branch from tag, make fix, test changes, merge in to main branch and create tag for hotfix.
```bash
git checkout -b hotfix-IssueId-X.X.X tags/vX.X.X
```
### Note: Users following naming convention for branches and tags.
1. For Tags vMajor.Minor.BuildNumber
2. For Release branch release-branch-Major.Minor.BuildNumber
3. For Hotfix branch hotfix-IssueId-Major.Minor.BuildNumber
4. For Feature branch feature-IssueId-IssueTitle
5. Always use mobile Build number which is odd number.(113, 213 etc.)