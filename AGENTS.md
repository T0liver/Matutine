## Android emulator

This computer has an Android emulator for checking the real Android UI. Follow `.cursor/skills/verify-android-emulator/SKILL.md`. The SDK is `~/Android/Sdk` and the virtual device is `Medium_Phone`.

When this session is running inside IntelliJ IDEA, use the IDE's MCP server for the emulator. `get_run_configurations` lists the Android run configuration. `execute_run_configuration` runs it, which starts `Medium_Phone`, installs the app, and launches it. `xdebug_start_debugger_session` debugs that same configuration. If those tools are hidden, call them through `execute_tool`. Do not start a second emulator from the shell while IntelliJ is driving one.

In committed files, commits, and review comments, write `~/Android/Sdk`. Do not write an absolute home-directory path.
