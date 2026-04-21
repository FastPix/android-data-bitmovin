# Changelog

All notable changes to this project will be documented in this file.

## [1.0.3]
- Upgrades Core SDK to 1.3.0

## [1.0.2]
### Changed
- Upgraded FastPix Core dependency to `1.2.9` in `gradle/libs.versions.toml`.
- Updated `FastPixBitMovinPlayer` pulse lifecycle handling to schedule pulse events during active playback states (`viewBegin`, `play`, `buffering`) instead of cancelling them.
- Updated seek completion behavior to keep pulse events active when playback resumes after seek, and cancel only when the player remains paused.
- Removed redundant pulse start/stop calls for `variantChanged`, `seeking`, `playerReady`, and `buffered` event dispatch paths.

## [1.0.1]
### Changed
- Bumped SDK version to `1.0.1` in `README.md`, `BitMovinLibraryInfo.kt`, and `build.gradle.kts`.
- Refactored `FastPixBitMovinPlayer` to use `FastPixAnalytics` for SDK initialization and management.
- Implemented periodic `pulse` event logic using Coroutines to track active playback.
- Added event lifecycle management to schedule or cancel pulse events based on player state (e.g., playing, buffering, seeking).
- Updated internal `PlayerListener` method signatures for `sourceFps` and `sourceAdvertiseFrameRate` to return `Int?`.
- Upgraded project dependencies including Kotlin to `2.1.0` and FastPix Core to `1.2.7`.

## [1.0.0] - Initial Release

### Added
- Initial release of FastPix Bitmovin Player Data Collector
- Integration with Bitmovin Player 3.0+ for automatic analytics tracking
- Automatic event tracking (play, pause, seeking, buffering, errors, quality changes, etc.)
- State transition validation for accurate event tracking
- Player information collection (dimensions, playhead time, duration, etc.)
- Error handling and reporting
- Resource lifecycle management

