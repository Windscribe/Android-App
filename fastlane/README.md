fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

### test

```sh
[bundle exec] fastlane test
```

Runs tests

### updateBuildVersion

```sh
[bundle exec] fastlane updateBuildVersion
```

Pull Build number from play store and update gradle.

### buildAndSign

```sh
[bundle exec] fastlane buildAndSign
```

Build and sign

### buildAndDebug

```sh
[bundle exec] fastlane buildAndDebug
```

Build debug apk

### publishToInternal

```sh
[bundle exec] fastlane publishToInternal
```

Publish app to play store (Internal channel)

### buildReleaseApk

```sh
[bundle exec] fastlane buildReleaseApk
```

Build Signed Apks for all modules.

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
