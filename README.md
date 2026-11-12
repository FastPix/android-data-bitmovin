# FastPix Video Data SDK for Bitmovin Player - monitor playback performance and QoE on Android

[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.4-green.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](bitmovin-player-data/build.gradle.kts)

The **FastPix Video Data SDK for Bitmovin Player** is a third-party analytics SDK that monitors video playback performance, quality of service (QoS), quality of experience (QoE), and user interactions in **Bitmovin Player on Android**. It attaches to your Bitmovin `PlayerView`, automatically tracks playback events, and reports real-time metrics - startup time, rebuffering, bitrate, and errors - to the [FastPix dashboard](https://dashboard.fastpix.com).

It is **not a video player** and does not replace Bitmovin Player. It is a third-party data platform you add alongside the Bitmovin Player you already ship - the same role Mux Data plays - so you can monitor playback quality and viewer engagement without building an analytics pipeline.

**Works with:** Bitmovin Player Android SDK 3.x · Android 7.0+ (API 24) · Kotlin and Java · `io.fastpix.data:bitmovin`

[Monitor Bitmovin Player docs](https://fastpix.com/docs/video-data/monitors/bitmovin-player) · [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup)

## Contents

| Get started | Understand | Reference |
|---|---|---|
| [Requirements](#requirements) | [Why FastPix](#why-fastpix) | [Detailed configuration](#detailed-configuration) |
| [Install the SDK](#installation) | [Built-in analytics vs FastPix](#built-in-bitmovin-analytics-vs-fastpix) | [Which FastPix SDK?](#which-fastpix-sdk-for-which-android-player) |
| [Configure your Bitmovin Player license](#configure-your-bitmovin-player-license) | [What you can track](#what-you-can-track) | [FAQ](#faq) |
| [Quick start](#quick-start) | [Who it's for](#who-its-for) | [Troubleshooting](#troubleshooting) |
| [Complete sample](#complete-sample-player-implementation) | | |

## Why FastPix

Bitmovin Player fires playback events, but turning them into viewer-facing quality metrics, storing them, and watching them across devices and app versions is work you would otherwise build yourself. FastPix does it for you:

- **Real-time QoE monitoring.** Startup time, rebuffering, bitrate and resolution changes, and errors as they happen on real devices, in one dashboard.
- **Automatic event tracking.** The SDK listens to Bitmovin Player's native events, so you keep full control of playback and add monitoring with a few lines.
- **One data model across platforms.** The same FastPix Video Data metrics are reported from web, iOS, Android, and other player integrations, so you compare experiences like for like.
- **Your metadata, your dashboard.** Tag views with video, player, and up to ten custom fields and analyze them in the FastPix dashboard.

## Built-in Bitmovin analytics vs FastPix

Bitmovin ships its own analytics and observability suite that tracks QoS/QoE, startup time, and buffering across its players. FastPix is a **third-party data platform** you run alongside the Bitmovin Player when you want:

- Bitmovin playback data unified with your other FastPix-monitored players (web, iOS, ExoPlayer, Media3, THEOplayer, and more).
- Custom dimensions and your own metadata on every view.
- One dashboard, API, and alerting surface across every platform you ship.

Use Bitmovin's built-in analytics, FastPix, or both - they are not mutually exclusive. Mux Data fills the same third-party role; FastPix reports the same class of QoE metrics and unifies them across your players.

## What you can track

- **Automatic event tracking** - automatically captures all playback events (play, pause, seek, buffering, etc.)
- **Bitmovin Player integration** - built specifically for the Bitmovin Player Android SDK
- **Real-time analytics** - instant access to video performance metrics on the FastPix dashboard
- **Minimal setup** - easy integration with just a few lines of code
- **Custom metadata** - support for custom video and player metadata

## Who it's for

- **Android teams shipping Bitmovin Player** who want production QoE monitoring without building an analytics backend.
- **Streaming, OTT, and broadcast apps** that need to catch rebuffering, slow startup, and playback errors before users churn.
- **Teams standardizing analytics across platforms** who want Bitmovin data in the same FastPix dashboard as their web and iOS players.

## Requirements

- **Minimum Android SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35+
- **Bitmovin Player SDK**: 3.x (integrated in your app, including Bitmovin's Maven repository `https://artifacts.bitmovin.com/artifactory/public-releases`)
- **A Bitmovin Player license**: required to play content, with your app's package name allowlisted (see [Configure your Bitmovin Player license](#configure-your-bitmovin-player-license))
- **GitHub Packages credentials**: the FastPix SDKs are published to GitHub Packages, so you need a GitHub username and a personal access token with the `read:packages` scope to download them (see [Configure authentication](#configure-authentication))
- **Kotlin**: 2.0.21+
- **Java**: 11

You will also need a FastPix account and a workspace key. [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup), then follow [set up a workspace](https://fastpix.com/docs/getting-started/set-up-a-workspace) to find your workspace key.

## Installation

### Add the repositories

In your project's `settings.gradle.kts`, add all three repositories the SDK needs. Bitmovin is not on Maven Central, and the FastPix core library lives in its own GitHub Packages repository:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // Bitmovin Player (not on Maven Central)
        maven { url = uri("https://artifacts.bitmovin.com/artifactory/public-releases") }
        // FastPix Bitmovin SDK
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-data-bitmovin")
            credentials {
                val props = java.util.Properties()
                val f = file("${rootDir}/local.properties")
                if (f.exists()) f.inputStream().use { props.load(it) }
                username = props.getProperty("gpr.user")
                password = props.getProperty("gpr.key")
            }
        }
        // FastPix core (transitive dependency of the Bitmovin SDK)
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-core-data-sdk")
            credentials {
                val props = java.util.Properties()
                val f = file("${rootDir}/local.properties")
                if (f.exists()) f.inputStream().use { props.load(it) }
                username = props.getProperty("gpr.user")
                password = props.getProperty("gpr.key")
            }
        }
    }
}
```

### Configure authentication

Create or update `local.properties` in your project root with your GitHub credentials. This file is git-ignored, so your token is never committed:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

Use a GitHub personal access token with the `read:packages` scope.

### Add Dependencies

Add the FastPix Bitmovin Player SDK to your app's `build.gradle.kts`. The Bitmovin Player SDK (`com.bitmovin.player:player`) and the FastPix core library resolve automatically as transitive dependencies, provided the repositories above are configured:

```kotlin
dependencies {
    // FastPix Bitmovin Player SDK (pulls in Bitmovin Player + FastPix core)
    implementation("io.fastpix.data:bitmovin:1.0.4")
}
```

> **Tip:** pin an explicit Bitmovin Player version rather than a dynamic range (for example `3.112.0`, not `3.+`). A dynamic range can resolve to a newer Bitmovin build whose transitive dependencies require a higher `compileSdk` than your project targets, breaking the build without warning.

## Configure your Bitmovin Player license

Bitmovin Player requires a license key to play any content. Without a valid key - with the app's package name allowlisted - the player shows:

> The license request was denied. Please visit the player section of the Bitmovin backend portal to make sure this domain is added to your player license.

1. Get your Bitmovin Player license key from the [Bitmovin dashboard](https://dashboard.bitmovin.com).
2. **Allowlist your app's package name** (your `applicationId`) under that player license in the Bitmovin dashboard.
3. Add the key to your `AndroidManifest.xml`, inside `<application>`:

   ```xml
   <meta-data
       android:name="BITMOVIN_PLAYER_LICENSE_KEY"
       android:value="YOUR_BITMOVIN_PLAYER_LICENSE_KEY"/>
   ```

   Alternatively, pass it in code when you build the player:

   ```kotlin
   val player = Player(
       context = this,
       playerConfig = PlayerConfig(key = "YOUR_BITMOVIN_PLAYER_LICENSE_KEY"),
   )
   ```

Attach the FastPix SDK to the licensed player exactly as shown in [Quick start](#quick-start).

## Quick Start

### Add Bitmovin PlayerView to Your Layout

Add the `Bitmovin PlayerView` to your activity/fragment layout:

```xml
<com.bitmovin.player.PlayerView
    android:id="@+id/bitmovin_player_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    app:initialize_player="false"
    app:use_controller="false"
    app:auto_show="false" />
```

### Initialize FastPix SDK in Your Activity

Here's a complete example of how to integrate FastPix SDK with Bitmovin Player. Make sure your Bitmovin Player license is configured first (see [Configure your Bitmovin Player license](#configure-your-bitmovin-player-license)), and replace `workSpaceId` with your real workspace key from the [FastPix dashboard](https://dashboard.fastpix.com):

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.bitmovin_player_data.src.CustomerData
import io.fastpix.bitmovin_player_data.src.FastPixBitMovinPlayer
import io.fastpix.data.domain.model.VideoDataDetails
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmovinData: FastPixBitMovinPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configure Bitmovin Player first
        configureBitmovinPlayer()

        // Then configure FastPix SDK
        configureFastPix()
    }

    private fun configureBitmovinPlayer() {
        val player = Player(context = this)
        binding.bitmovinPlayerView.player = player
        binding.bitmovinPlayerView.setUiVisible(false)
        val source = Source(SourceConfig(
            url = "your-video-url",
            type = SourceType.Hls,
            title = "video-title",),)
        player.load(source)
        player.play()
        setupPlayerListeners()
    }

    private fun configureFastPix() {
        // Create video metadata
        val videoDataDetails = VideoDataDetails(
            videoId = "video-id",
            videoTitle = "Sample Video",
            videoSeries = "Sample Series",
            videoProducer = "Producer Name",
            videoContentType = "Video Content Type",
            videoVariant = "HD",
            videoLanguage = "en",
            videoDrmType = null // or "Widevine", "FairPlay", etc.
        )

        // Create customer data configuration
        val customerData = CustomerData(
            workSpaceId = "YOUR_WORKSPACE_KEY", // Your FastPix workspace key
            beaconUrl = null, // Optional: Custom beacon URL
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails(
                playerName = "Bitmovin",
                playerVersion = "3.112.0" // Your Bitmovin Player version
            ),
            customDataDetails = CustomDataDetails(
                customField1 = "custom-value-1",
                customField2 = "custom-value-2"
                // Add up to customField10 if needed
            )
        )

        // Initialize FastPix SDK
        bitmovinData = FastPixBitMovinPlayer(
          this,
          binding.bitmovinPlayerView,
          binding.bitmovinPlayerView.player,
          enableLogging = true,
          customerData = customerData
        )
    }

    override fun onResume() {
        super.onResume()
        binding.bitmovinPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.bitmovinPlayerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Important: Release BitmovinPlayer and FastPix SDK
        binding.bitmovinPlayerView.unLoad()
        bitmovinData?.release()
    }
}
```

## Detailed Configuration

### CustomerData Parameters

The `CustomerData` class accepts the following parameters:

| Parameter           | Type              | Required | Description                                            |
|---------------------|-------------------|----------|--------------------------------------------------------|
| `workSpaceId`       | String            | ✅        | Your FastPix workspace key                             |
| `beaconUrl`         | String            | ❌        | Custom beacon URL (default: metrix.ws)                 |
| `videoDetails`      | VideoDataDetails  | ❌        | Video metadata (see below)                             |
| `playerData`        | PlayerDataDetails | ❌        | Player information (default: "bitmovin-player", "3.+") |
| `customDataDetails` | CustomDataDetails | ❌        | Custom metadata fields                                 |

### VideoDataDetails

Configure video metadata for better analytics:

```kotlin
val videoDataDetails = VideoDataDetails(
    videoId = "unique-video-id",           // Optional
    videoTitle = "Video Title",            // Optional
    videoSeries = "Series Name",           // Optional
    videoProducer = "Producer Name",       // Optional
    videoContentType = "Movie/TV Show",    // Optional
    videoVariant = "HD/SD/4K",             // Optional
    videoLanguage = "en",                  // Optional
    videoDrmType = "Widevine"              // Optional
)
```

### CustomDataDetails

Add custom metadata fields (up to 10 fields):

```kotlin
val customDataDetails = CustomDataDetails(
    customField1 = "value1",
    customField2 = "value2",
    // ... up to customField10
)
```

## Complete Sample Player Implementation

Here's a more complete example with custom controls:

```kotlin
class VideoPlayerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var bitmovinData: FastPixBitMovinPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

      // Configure Bitmovin Player first
      initializePlayer()

      // Then configure FastPix SDK
      configureFastPix()
    }

    private fun initializePlayer() {
        val player = Player(context = this)
        binding.bitmovinPlayerView.player = player
        binding.bitmovinPlayerView.setUiVisible(false)
        val source = Source(
          SourceConfig(
            url = "video-url",
            type = SourceType.Hls,
            title = "video-title",
          ),
        )
        player.load(source)
        player.play()
        setupPlayerListeners()
    }

    private fun setupFastPix() {
        val videoDataDetails = VideoDataDetails(
            videoId = "video-id",
            videoTitle = "video-title",
            videoSeries = "Sample Series",
            videoProducer = "Sample Producer",
            videoContentType = "Video Content",
            videoVariant = "HD",
            videoLanguage = "en"
        )

        val customerData = CustomerData(
            workSpaceId = "YOUR_WORKSPACE_KEY",
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails("bitmovin", "3.112.0"),
            customDataDetails = CustomDataDetails(
                customField1 = "custom-data-1"
            )
        )
      
        bitmovinData = FastPixBitMovinPlayer(
          this,
          binding.bitmovinPlayerView,
          binding.bitmovinPlayerView.player,
          enableLogging = true,
          customerData = customerData
        )
    }

    override fun onResume() {
        super.onResume()
        binding.bitmovinPlayerView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.bitmovinPlayerView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.bitmovinPlayerView.onDestroy()
        bitmovinData?.release()
        bitmovinData = null
    }
}
```

## Lifecycle Management

It's crucial to properly manage the SDK lifecycle:

1. **Initialize** the SDK after BitmovinPlayer is configured
2. **Call `onResume()`** and `onPause()` on BitmovinPlayerView in your activity lifecycle
3. **Always call `release()`** in `onDestroy()` to clean up resources

```kotlin
override fun onDestroy() {
    super.onDestroy()
    binding.bitmovinPlayerView.unLoad()
    bitmovinData?.release()
}
```

## Debugging

Enable logging during development:

```kotlin
bitmovinData = FastPixBitMovinPlayer(
  this,
  binding.bitmovinPlayerView,
  binding.bitmovinPlayerView.player,
  enableLogging = true,
  customerData = customerData
)
```

Logs will appear in Logcat with the tag `FastPixBitMovinPlayer`.

## Troubleshooting

### "The license request was denied" / video does not play

- This is a Bitmovin Player license error, not a FastPix error. Configure your Bitmovin Player license and make sure your app's package name is allowlisted in the Bitmovin dashboard. See [Configure your Bitmovin Player license](#configure-your-bitmovin-player-license).

### Build fails: "requires ... compile against version 37 or later of the Android APIs"

- A dynamic Bitmovin version (`3.+`) resolved to a build whose transitive dependencies need a higher `compileSdk`. Pin an explicit Bitmovin Player version (for example `3.112.0`), or raise your `compileSdk` and Android Gradle plugin. See [Add Dependencies](#add-dependencies).

### Dependencies fail to resolve

- Confirm all three repositories are configured (Bitmovin, FastPix Bitmovin SDK, FastPix core) - see [Installation](#installation)
- Confirm `gpr.user` / `gpr.key` are set in `local.properties` and your token has the `read:packages` scope

### SDK Not Tracking Events

- Ensure you've initialized the SDK after configuring BitmovinPlayer
- Check that `workSpaceId` is correct
- Verify BitmovinPlayer events are firing (check BitmovinPlayer logs)
- Enable logging to see FastPix SDK activity

### Memory Leaks

- Always call `release()` in `onDestroy()`
- Ensure `BitmovinPlayerView.onDestroy()` is called before releasing FastPix SDK

### Missing Events

- The SDK automatically tracks all events from BitmovinPlayer
- Events are tracked based on BitmovinPlayer's native event system
- Check that BitmovinPlayer is properly configured and receiving events

## Which FastPix SDK for which Android player

FastPix ships a Video Data SDK per player. Pick the one that matches the player your app ships:

| Your Android player | Use this SDK |
| --- | --- |
| **Bitmovin Player** | **FastPix/android-data-bitmovin** (this repository) |
| **AndroidX Media3** (the current ExoPlayer) | [FastPix/android-data-androidXmedia3](https://github.com/FastPix/android-data-androidXmedia3) |
| **Google ExoPlayer** (standalone / legacy) | [FastPix/android-data-exoplayer-sdk](https://github.com/FastPix/android-data-exoplayer-sdk) |
| **THEOplayer** | [FastPix/android-data-theoplayer](https://github.com/FastPix/android-data-theoplayer) |
| **Brightcove Player** | [FastPix/android-data-brightcove](https://github.com/FastPix/android-data-brightcove) |
| Any player, shared core layer | [FastPix/android-core-data-sdk](https://github.com/FastPix/android-core-data-sdk) |

## FAQ

**How do I monitor Bitmovin Player performance on Android?**
Add the FastPix Video Data SDK, then create a `FastPixBitMovinPlayer` with your Bitmovin `PlayerView`, player instance, and workspace key. It automatically tracks playback events and reports real-time QoS/QoE metrics to the FastPix dashboard. See [Install the SDK](#installation) and [Quick start](#quick-start).

**Do I need a Bitmovin license?**
Yes. Bitmovin Player requires a license key with your app's package name allowlisted, or playback is denied. This is separate from your FastPix workspace key. See [Configure your Bitmovin Player license](#configure-your-bitmovin-player-license).

**Does Bitmovin have built-in analytics, and how is this different?**
Yes - Bitmovin has its own analytics and observability suite. FastPix is a third-party data platform you run alongside it to unify Bitmovin playback data with your other players, add custom dimensions, and use one dashboard and API across platforms. See [Built-in analytics vs FastPix](#built-in-bitmovin-analytics-vs-fastpix).

**What metrics does it track?**
Playback and quality-of-experience signals: play/pause/seek/buffering events, startup time, rebuffering, bitrate and resolution changes, errors, and viewer engagement, plus any custom metadata you attach.

**Is this a video player?**
No. It does not play or render video. It is a monitoring SDK that attaches to the Bitmovin Player you already use.

**How does it compare to Mux Data for Bitmovin Player?**
Both are third-party Video Data platforms for Bitmovin Player. FastPix reports the same class of QoE metrics and unifies Bitmovin data with your web, iOS, and other FastPix-monitored players in one dashboard.

**Do I need a FastPix account?**
Yes. You need a workspace key from your FastPix account to report data. [Create a free account](https://dashboard.fastpix.com/signup?utm_source=github&utm_medium=referral&utm_campaign=github_signup).

## Documentation

- [Monitor Bitmovin Player - FastPix docs](https://fastpix.com/docs/video-data/monitors/bitmovin-player)
- [FastPix Video Data overview](https://fastpix.com/docs/video-data/overview)
- [Use custom dimensions](https://fastpix.com/docs/video-data/use-custom-dimensions)
- [Bitmovin Player Android docs](https://developer.bitmovin.com/playback/docs/getting-started-android)

## Support

For questions, issues, or feature requests:

- **Email**: support@fastpix.com
- **Documentation**: [FastPix Documentation](https://fastpix.com/docs)
- **GitHub Issues**: [Report an issue](https://github.com/FastPix/android-data-bitmovin/issues)

## License

Copyright © 2025 FastPix. All rights reserved.

This SDK is proprietary software. Unauthorized copying, modification, distribution, or use of this
software is strictly prohibited.
