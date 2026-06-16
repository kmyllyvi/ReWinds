Run the Maestro UI test suite locally against a connected Android emulator or iOS simulator.

## Usage

`/run-ui-tests` — runs all flows in `.maestro/`
`/run-ui-tests j6` — runs a specific flow by name prefix (e.g. `j6_ask_ai_chat_question.yaml`)

## What this does

1. Check that `maestro` is installed and at version **1.39.0** or newer (`maestro --version`). If missing or outdated, tell the user to install/upgrade: `curl -fsSL https://get.maestro.mobile.dev | MAESTRO_VERSION="1.39.0" bash` then reopen the terminal.
2. If an argument was given (`$ARGUMENTS`), find the matching flow file under `.maestro/flows/` and run only that file. Otherwise run the full suite with `maestro test .maestro/`.
3. Report pass/fail per flow. If any flow fails, show the Maestro error output.

## Notes

- Android: requires a running emulator with the debug APK installed (`./gradlew :composeApp:assembleDebug` then `adb install -r ...apk`). App ID is `com.km.rewinds`.
- iOS: requires a booted simulator with the app installed via Xcode.
- Flow J6 (`j6_ask_ai_chat_question.yaml`) hits the live Anthropic API — needs `MAESTRO_APP_ANTHROPIC_KEY` set in the environment.
- Do not attempt to build the app as part of this command — tell the user to build first if the app is not already installed.

$ARGUMENTS
