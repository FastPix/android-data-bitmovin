# FastPix Bitmovin Player SDK

[![License](https://img.shields.io/badge/License-Proprietary-blue.svg)](LICENSE)
[![Version](https://img.shields.io/badge/version-1.0.0-green.svg)](CHANGELOG.md)
[![Min SDK](https://img.shields.io/badge/minSdk-24-orange.svg)](bitmovin-player-data/build.gradle.kts)

The FastPix Bitmovin Player SDK provides seamless integration between Bitmovin Player and the FastPix
analytics platform. This SDK automatically tracks video playback events, metrics, and analytics data
from your Bitmovin Player instances, enabling real-time monitoring and insights on the FastPix dashboard.

## Key Features

- **Automatic Event Tracking** – Automatically captures all playback events (play, pause, seek,
  buffering, etc.)
- **Bitmovin Player Integration** – Built specifically for Bitmovin Player Android SDK
- **Real-time Analytics** – Provides instant access to video performance metrics on the FastPix
  dashboard
- **Minimal Setup** – Easy integration with just a few lines of code
- **Custom Metadata** – Support for custom video and player metadata

## Requirements

- **Minimum Android SDK**: 24 (Android 7.0)
- **Target/Compile SDK**: 35+
- **Bitmovin Player SDK**: 5.0+
- **Kotlin**: 2.0.21+
- **Java**: 11

## Installation

### Step 1: Add GitHub Packages Repository

Add the GitHub Packages repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/FastPix/android-data-bitmovin")
            credentials {
                username =
                    project.findProperty("lpr.user")
                password =
                    project.findProperty("lpr.key")
            }
        }
    }
}
```

### Step 2: Add Dependencies

Add the FastPix Bitmovin Player SDK and Bitmovin Player dependencies to your app's `build.gradle.kts`:

```kotlin
dependencies {
    // FastPix Bitmovin Player SDK
    implementation("io.fastpix.data:bitmovin:1.0.0")
}
```

### Step 3: Configure Authentication

Create or update `local.properties` in your project root with your GitHub credentials:

```properties
lpr.user=YOUR_GITHUB_USERNAME
lpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

> **Note**: Make sure to add `local.properties` to your `.gitignore` to keep credentials secure.

## Quick Start

### 1. Add Bitmovin PlayerView to Your Layout

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

### 2. Initialize FastPix SDK in Your Activity

Here's a complete example of how to integrate FastPix SDK with Bitmovin Player:

```kotlin
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
            workSpaceId = "workspace-id", // Your FastPix workspace ID
            beaconUrl = null, // Optional: Custom beacon URL
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails(
                playerName = "Bitmovin",
                playerVersion = "player-version" // Your BitmovinPlayer version
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
| `workSpaceId`       | String            | ✅        | Your FastPix workspace identifier                      |
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
            workSpaceId = "workspace-id",
            videoDetails = videoDataDetails,
            playerData = PlayerDataDetails("bitmovin", "player-version"),
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

## Support

For questions, issues, or feature requests:

- **Email**: support@fastpix.io
- **Documentation**: [FastPix Documentation](https://docs.fastpix.io)
- **GitHub Issues**: [Report an issue](https://github.com/FastPix/android-data-bitmovin/issues)

## License

Copyright © 2025 FastPix. All rights reserved.

This SDK is proprietary software. Unauthorized copying, modification, distribution, or use of this
software is strictly prohibited.