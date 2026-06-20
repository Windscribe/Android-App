# Healthcheck

Run the project's local verification pipeline in order. Each stage must pass before moving to the next — if a stage fails, stop, report the failure with the relevant Gradle output, and do not continue to later stages.

Run every Gradle command from the project root with `./gradlew`. Append `2>&1 | tail -40` (or more) so failures are visible. These are long-running; allow generous timeouts.

## Stage 1 — ktlint (format if needed)

1. Run `./gradlew ktlintCheck`.
2. If it fails on formatting violations, run `./gradlew ktlintFormat`, then run `git status --short` to show which files were reformatted, and re-run `./gradlew ktlintCheck` to confirm it now passes.
3. Report any files the formatter changed so the user can review them.

## Stage 2 — Unit tests (all modules)

Run the unit tests for every module that has them. The modules use different variant names, so they must be named explicitly — `testGoogleDebugUnitTest` only matches `:base` and silently skips the vendor modules:

```
./gradlew :base:testGoogleDebugUnitTest :openvpn:testSkeletonDebugUnitTest :wgtunnel:testDebugUnitTest
```

- `:base` has the `google` flavor → `testGoogleDebugUnitTest`
- `:openvpn` has no `google` flavor (uses `skeleton`) → `testSkeletonDebugUnitTest`
- `:wgtunnel` has no flavors → `testDebugUnitTest`
- `:mobile`, `:tv`, `:strongswan` have no unit test sources

Report pass/fail counts. On failure, surface the failing test names and the assertion output — do not proceed to builds.

## Stage 3 — Google debug builds

```
./gradlew :mobile:assembleGoogleDebug :tv:assembleGoogleDebug
```

## Stage 4 — fdroid debug builds

```
./gradlew :mobile:assembleFdroidDebug :tv:assembleFdroidDebug
```

## Final report

After all stages pass, give a concise summary: ktlint clean (and whether anything was reformatted), unit test result, and that both google and fdroid debug builds for mobile + tv succeeded. If any stage failed, the summary should make clear which stage stopped the run and why.