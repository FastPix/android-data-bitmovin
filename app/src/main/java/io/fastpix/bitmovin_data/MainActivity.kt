package io.fastpix.bitmovin_data

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowInsetsController
import android.widget.SeekBar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.bitmovin.analytics.api.AnalyticsConfig
import com.bitmovin.analytics.api.SourceMetadata
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.analytics.AnalyticsPlayerConfig
import com.bitmovin.player.api.analytics.AnalyticsSourceConfig
import com.bitmovin.player.api.event.PlayerEvent.Error
import com.bitmovin.player.api.event.PlayerEvent.Paused
import com.bitmovin.player.api.event.PlayerEvent.Play
import com.bitmovin.player.api.event.PlayerEvent.Playing
import com.bitmovin.player.api.event.PlayerEvent.Ready
import com.bitmovin.player.api.event.PlayerEvent.Seek
import com.bitmovin.player.api.event.PlayerEvent.Seeked
import com.bitmovin.player.api.event.PlayerEvent.StallEnded
import com.bitmovin.player.api.event.PlayerEvent.StallStarted
import com.bitmovin.player.api.source.Source
import com.bitmovin.player.api.source.SourceConfig
import com.bitmovin.player.api.source.SourceType
import io.fastpix.bitmovin_data.databinding.ActivityMainBinding
import io.fastpix.bitmovin_player_data.src.CustomerData
import io.fastpix.bitmovin_player_data.src.FastPixBitMovinPlayer
import io.fastpix.data.domain.model.VideoDataDetails
import java.util.Locale
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var analyticsLicenseKey = "5b86ed6d-5698-470f-81eb-0074dfbc384a"
    private var isSeeking = false
    private val seekBarUpdateHandler = Handler(Looper.getMainLooper())
    private var seekBarUpdateRunnable: Runnable? = null
    private val controlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    private var isFullscreen = false
    private var bitmovinData: FastPixBitMovinPlayer? = null
    private var controlsVisible = true
    private var videoModel: DummyData? = null
    private var currentVideoIndex: Int = -1
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        videoModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("video_model", DummyData::class.java)
        } else {
            intent.getParcelableExtra("video_model")
        }
        // Find current video index in the list
        currentVideoIndex =
            dummyData.indexOfFirst { it.id == videoModel?.id && it.url == videoModel?.url }
        if (currentVideoIndex == -1 && videoModel != null) {
            // Fallback: try to find by id only
            currentVideoIndex = dummyData.indexOfFirst { it.id == videoModel?.id }
        }
        initializePlayer()
        setupControls()
        setUpFastPix()
    }

    private fun showControls() {
        controlsVisible = true
        binding.controlsContainer.visibility = View.VISIBLE
        startHideControlsTimer()
    }

    private fun hideControls() {
        controlsVisible = false
        binding.controlsContainer.visibility = View.GONE
    }


    private fun toggleControls() {
        if (controlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun setUpFastPix() {
        val customerData = CustomerData(
            workspaceId = "1129455265676623875",
            beaconUrl = "metrix.ninja",
            videoDetails = VideoDataDetails(
                UUID.randomUUID().toString(),
                videoModel?.id,
                videoCDN = "cloudflare"
            )
        )
        bitmovinData = FastPixBitMovinPlayer(
            this,
            binding.bitmovinPlayerView,
            binding.bitmovinPlayerView.player!!,
            enableLogging = true,
            customerData = customerData
        )
    }

    private fun initializePlayer() {
        try {
            val analyticsConfig = AnalyticsConfig(
                licenseKey = analyticsLicenseKey,
            )
            val player = Player(
                context = this,
                analyticsConfig = AnalyticsPlayerConfig.Enabled(analyticsConfig),
            )
            binding.bitmovinPlayerView.player = player
            // Hide the default Bitmovin UI controls
            binding.bitmovinPlayerView.setUiVisible(false)
            val streamTitle = "Sintel"
            val source = Source(
                SourceConfig(
                    url = videoModel!!.url,
                    type = SourceType.Hls,
                    title = streamTitle,
                ),
                AnalyticsSourceConfig.Enabled(
                    SourceMetadata(
                        videoId = "android-wizard-Sintel-1763706788214",
                        title = streamTitle,
                    )
                ),
            )

            player.load(source)
            player.play()
            setupPlayerListeners()

        } catch (e: Exception) {
            Log.e("BITMOVIN_ERROR", "Failed to initialize player: ${e.message}", e)
        }
    }

    private fun setupPlayerListeners() {
        // Setup player listeners

        binding.bitmovinPlayerView.setOnClickListener {
            toggleControls()
        }

        binding.bitmovinPlayerView.player?.let { p ->
            p.on(Ready::class.java) {
                hideLoader()
                updateDuration()
                startSeekBarUpdates()
                // Ensure default UI stays hidden
                binding.bitmovinPlayerView.setUiVisible(false)
            }

            p.on(Play::class.java) {
                updatePlayPauseButton(true)
            }

            p.on(Playing::class.java) {
                updatePlayPauseButton(true)
            }

            p.on(Paused::class.java) {
                updatePlayPauseButton(false)
            }

            p.on(Seek::class.java) {
                showLoader()
            }

            p.on(Seeked::class.java) {
                hideLoader()
                updateSeekBar()
            }

            p.on(Error::class.java) { event ->
                val errorMessage = event.message ?: "Unknown error occurred"
                hideLoader()
            }

            p.on(StallStarted::class.java) {
                showLoader()
            }

            p.on(StallEnded::class.java) {
                hideLoader()
            }
        }
    }

    private fun setupControls() {
        // Play/Pause button
        binding.playPauseButton.setOnClickListener {
            binding.bitmovinPlayerView.player?.let { p ->
                if (p.isPlaying) {
                    p.pause()
                } else {
                    p.play()
                }
            }
        }

        // Seek bar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.bitmovinPlayerView.player?.let { p ->
                        val duration = p.duration
                        if (duration > 0) {
                            val position = (progress / 100.0) * duration
                            binding.currentTimeText.text = formatTime(position)
                        }
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                isSeeking = false
                binding.bitmovinPlayerView.player?.let { p ->
                    val duration = p.duration
                    if (duration > 0) {
                        val position = (seekBar?.progress ?: 0) / 100.0 * duration
                        p.seek(position)
                    }
                }
            }
        })

        // Forward seek (10 seconds)
        binding.forwardSeek.setOnClickListener {
            binding.bitmovinPlayerView.player?.let { p ->
                val currentTime = p.currentTime
                val newTime = currentTime + 10.0
                p.seek(newTime)
            }
        }

        // Backward seek (10 seconds)
        binding.backwardSeek.setOnClickListener {
            binding.bitmovinPlayerView.player?.let { p ->
                val currentTime = p.currentTime
                val newTime = maxOf(0.0, currentTime - 10.0)
                p.seek(newTime)
            }
        }

        // Next episode
        binding.nextEpisode.setOnClickListener {
            navigateToNextEpisode()
        }

        // Previous episode
        binding.previousEpisode.setOnClickListener {
            navigateToPreviousEpisode()
        }

        // Fullscreen button
        binding.fullscreenButton.setOnClickListener {
            toggleFullscreen()
        }

        startHideControlsTimer()
    }

    private fun navigateToPreviousEpisode() {
        if (currentVideoIndex > 0) {
            switchToEpisode(currentVideoIndex - 1)
        }
    }

    private fun navigateToNextEpisode() {
        if (currentVideoIndex >= 0 && currentVideoIndex < dummyData.size - 1) {
            switchToEpisode(currentVideoIndex + 1)
        }
    }


    private fun switchToEpisode(newIndex: Int) {
        if (newIndex < 0 || newIndex >= dummyData.size) {
            Log.e(TAG, "Invalid episode index: $newIndex")
            return
        }

        // Release existing players
        try {
            binding.bitmovinPlayerView.player?.unload()
            binding.bitmovinPlayerView.player?.destroy()
            // Release FastPix
            bitmovinData?.release()
            videoModel = dummyData[newIndex]
            currentVideoIndex = newIndex
            bitmovinData = null
            initializePlayer()
            // Reset UI
            resetPlayerUI()

            // Reinitialize players with new video
            setUpFastPix()
            updateEpisodeButtons()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing players: ${e.message}")
        }
    }

    private fun updateEpisodeButtons() {
        // Enable/disable buttons based on current position
        binding.nextEpisode.isEnabled =
            currentVideoIndex >= 0 && currentVideoIndex < dummyData.size - 1
        binding.previousEpisode.isEnabled = currentVideoIndex > 0

        // Optionally change alpha to show disabled state
        binding.nextEpisode.alpha = if (binding.nextEpisode.isEnabled) 1.0f else 0.5f
        binding.previousEpisode.alpha = if (binding.previousEpisode.isEnabled) 1.0f else 0.5f
    }

    private fun resetPlayerUI() {
        binding.seekBar.progress = 0
        binding.seekBar.max = 0
        binding.seekBar.secondaryProgress = 0
        binding.currentTimeText.text = formatTime(0.0)
        binding.durationText.text = formatTime(0.0)
        showLoader()
    }

    private fun showLoader() {
        binding.loadingIndicator.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        binding.loadingIndicator.visibility = View.GONE
    }

    private fun startHideControlsTimer() {
        controlsHandler.removeCallbacks(hideControlsRunnable)
        controlsHandler.postDelayed(hideControlsRunnable, 3000) // Hide after 3 seconds
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        binding.playPauseButton.setImageResource(
            if (isPlaying) {
                R.drawable.ic_pause
            } else {
                R.drawable.ic_play_arrow
            }
        )
    }

    private fun updateSeekBar() {
        binding.bitmovinPlayerView.player?.let { p ->
            val duration = p.duration
            val currentTime = p.currentTime
            if (duration > 0 && !isSeeking) {
                val progress = ((currentTime / duration) * 100).toInt()
                binding.seekBar.progress = progress
                binding.currentTimeText.text = formatTime(currentTime)
            }
        }
    }

    private fun startSeekBarUpdates() {
        stopSeekBarUpdates()
        seekBarUpdateRunnable = object : Runnable {
            override fun run() {
                updateSeekBar()
                seekBarUpdateHandler.postDelayed(this, 100) // Update every 100ms
            }
        }
        seekBarUpdateRunnable?.let { seekBarUpdateHandler.post(it) }
    }

    private fun stopSeekBarUpdates() {
        seekBarUpdateRunnable?.let { seekBarUpdateHandler.removeCallbacks(it) }
        seekBarUpdateRunnable = null
    }

    private fun updateDuration() {
        binding.bitmovinPlayerView.player?.let { p ->
            val duration = p.duration
            if (duration > 0) {
                binding.durationText.text = formatTime(duration)
                binding.seekBar.max = 100
            }
        }
    }

    private fun formatTime(timeSeconds: Double): String {
        val totalSeconds = timeSeconds.toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        hideSystemUI()
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen)
    }

    private fun exitFullscreen() {
        isFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        showSystemUI()
        binding.fullscreenButton.setImageResource(R.drawable.ic_fullscreen_exit)
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        } else {
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }
    }

    private fun showSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.show(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        } else {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_VISIBLE
        }
    }

    override fun onStart() {
        super.onStart()
        binding.bitmovinPlayerView.player?.let { p ->
            binding.bitmovinPlayerView.onStart()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bitmovinPlayerView.player?.let { p ->
            binding.bitmovinPlayerView.onResume()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.bitmovinPlayerView.player?.let { p ->
            binding.bitmovinPlayerView.onPause()
        }
    }

    override fun onStop() {
        super.onStop()
        binding.bitmovinPlayerView.player?.let { p ->
            binding.bitmovinPlayerView.onStop()
        }
    }

    override fun onDestroy() {
        stopSeekBarUpdates()
        bitmovinData?.release()
        binding.bitmovinPlayerView.player?.let { p ->
            binding.bitmovinPlayerView.onDestroy()
            p.unload()
        }
        binding.bitmovinPlayerView.player = null
        super.onDestroy()
    }
}