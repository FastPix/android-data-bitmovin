package io.fastpix.bitmovin_player_data.src

import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.View
import com.bitmovin.player.api.Player
import com.bitmovin.player.api.event.OfflineEvent
import com.bitmovin.player.api.event.PlayerEvent
import com.bitmovin.player.api.event.SourceEvent
import io.fastpix.bitmovin_player_data.src.info.BitMovinLibraryInfo
import io.fastpix.bitmovin_player_data.src.utils.PlayerEvents
import io.fastpix.bitmovin_player_data.src.utils.Utils
import io.fastpix.data.FastPixAnalytics
import io.fastpix.data.FastPixDataSDK
import io.fastpix.data.domain.SDKConfiguration
import io.fastpix.data.domain.enums.PlayerEventType
import io.fastpix.data.domain.listeners.PlayerListener
import io.fastpix.data.domain.model.BandwidthModel
import io.fastpix.data.domain.model.ErrorModel
import io.fastpix.data.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil

class FastPixBitMovinPlayer(
    private val context: Context,
    private val playerView: View,
    private val player: Player,
    private val enableLogging: Boolean = false,
    private val customerData: CustomerData
) : PlayerListener {
    private val TAG = "FastPixBitMovinPlayer"
    private var fastPixDataSDK: FastPixDataSDK? = null
    private var videoSourceHeight: Int = 0
    private var videoSourceWidth: Int = 0
    private var currentEventState: PlayerEvents? = null
    private var isReleased = false
    private var errorMessage: String? = null
    private var errorCode: String? = null
    private var lastSeekPosition: Double = 0.0
    private var currentPosition: Double = 0.0
    private var bandwidthModel: BandwidthModel? = null

    private val PULSE_INTERVAL = 10_000L // 1 second
    private val isPulseScheduled = AtomicBoolean(false)
    private val dispatcherScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pulseJob: Job? = null

    init {
        initializeFastPixDataSdk()
        setUpPlayerObservers()
        dispatchViewBegin()
        dispatchPlayerReady()
        dispatchPlay()
    }

    private fun cancelPulseEvent() {
        if (isPulseScheduled.get()) {
            Logger.log("EventDispatcher", "Cancelling pulse events")
            isPulseScheduled.set(false)
            pulseJob?.cancel()
        }
    }

    private fun schedulePulseEvents() {
        if (isPulseScheduled.get()) return

        isPulseScheduled.set(true)
        pulseJob = dispatcherScope.launch {
            while (isPulseScheduled.get()) {
                delay(PULSE_INTERVAL)
                if (isPulseScheduled.get()) {
                    withContext(Dispatchers.Main) {
                        fastPixDataSDK?.dispatchEvent(PlayerEventType.pulse)
                    }
                }
            }
        }
    }

    private fun setUpPlayerObservers() {
        player.on(PlayerEvent.VideoSizeChanged::class) { event ->
            videoSourceWidth = event.width
            videoSourceHeight = event.height
            dispatchVariantChange()
        }

        player.on(PlayerEvent.Play::class) {
            Log.e(TAG, "PlayerEvent.Play: ")
            dispatchPlay()
        }

        player.on(PlayerEvent.Playing::class) {
            Log.e(TAG, "PlayerEvent.Playing: ")
            dispatchPlaying()
        }

        player.on(PlayerEvent.Paused::class) {
            dispatchPause()
        }

        player.on(PlayerEvent.TimeChanged::class) { event ->
            currentPosition = event.time
        }

        player.on(PlayerEvent.DownloadFinished::class) { event ->
            bandwidthModel = BandwidthModel()
            bandwidthModel?.requestUrl = event.url
            dispatchRequestCompleted()
        }


        player.on(SourceEvent.DownloadFinished::class) { event ->
            bandwidthModel = BandwidthModel()
            bandwidthModel?.requestUrl = event.url
            dispatchRequestCompleted()
        }


        player.on(PlayerEvent.Seek::class) { event ->
            lastSeekPosition = event.to.time
            dispatchPause()
            dispatchSeeking()
        }

        player.on(PlayerEvent.Seeked::class) { event ->
            dispatchSeeked()
        }

        player.on(PlayerEvent.StallStarted::class) {
            dispatchBuffering()
        }

        player.on(PlayerEvent.StallEnded::class) {
            dispatchBufferedEvent()
        }

        player.on(PlayerEvent.PlaybackFinished::class) {
            dispatchEnded()
        }
        player.on(SourceEvent.Error::class) { event ->
            errorCode = event.code.value.toString()
            errorMessage = event.message
            dispatchError()
        }

        player.on(SourceEvent.Warning::class) { event ->
            errorCode = event.code.value.toString()
            errorMessage = event.message
            dispatchError()
        }

        player.on(OfflineEvent.Error::class) { event ->
            errorCode = event.code.value.toString()
            errorMessage = event.message
            dispatchError()
        }

        player.on(OfflineEvent.Warning::class) { event ->
            errorCode = event.code.value.toString()
            errorMessage = event.message
            dispatchError()
        }


    }

    private fun dispatchRequestCompleted() {
        if (isReleased || fastPixDataSDK == null) return
        if (enableLogging) {
            Log.d(TAG, "Dispatching Request Completed event")
        }
        fastPixDataSDK?.dispatchEvent(PlayerEventType.requestCompleted)
    }

    private fun initializeFastPixDataSdk() {
        val sdkConfiguration = SDKConfiguration(
            workspaceId = customerData.workspaceId,
            beaconUrl = customerData.beaconUrl,
            playerData = customerData.playerDetails,
            videoData = customerData.videoDetails,
            playerListener = this,
            enableLogging = enableLogging,
            customData = customerData.customDataDetails
        )
        FastPixAnalytics.initialize(sdkConfiguration, context)
        fastPixDataSDK = FastPixAnalytics.getSDK()
    }

    private fun dispatchBufferedEvent() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.BUFFERED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Buffered event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.buffered)
        }
    }

    private fun dispatchBuffering() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.BUFFERING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Buffering event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.buffering)
        }
    }

    private fun dispatchError() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.ERROR)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Error event:")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.error)
        }
    }

    private fun dispatchEnded() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.ENDED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Ended event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.ended)
        }
    }

    private fun dispatchSeeked() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.SEEKED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Seeked event ${Utils.secondToMs(lastSeekPosition)}")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(
                PlayerEventType.seeked,
                Utils.secondToMs(lastSeekPosition)
            )
        }
    }

    private fun dispatchSeeking() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.SEEKING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Seeking event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.seeking)
        }
    }

    private fun dispatchPause() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.PAUSE)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Pause event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.pause)
        }
    }

    private fun dispatchVariantChange() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.VARIANT_CHANGED)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching VariantChange event")
            }
            schedulePulseEvents()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.variantChanged)
        }
    }

    /**
     * Safely transitions to a new event state if valid
     */
    private fun transitionToEvent(newEvent: PlayerEvents): Boolean {
        if (isValidTransition(newEvent)) {
            if (newEvent != PlayerEvents.VARIANT_CHANGED) {
                currentEventState = newEvent
            }
            return true
        } else {
            return false
        }
    }

    /**
     * Validates if the transition from current state to new state is valid
     */
    private fun isValidTransition(newEvent: PlayerEvents): Boolean {
        val allowedTransitions = Utils.validTransitions[currentEventState] ?: emptySet()
        return newEvent in allowedTransitions
    }

    private fun dispatchPlaying() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.PLAYING)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Playing event")
            }
            schedulePulseEvents()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.playing)
        }
    }

    private fun dispatchPlayerReady() {
        if (isReleased || fastPixDataSDK == null) return
        if (enableLogging) {
            Log.d(TAG, "Dispatching Play Ready event")
        }
        cancelPulseEvent()
        fastPixDataSDK?.dispatchEvent(PlayerEventType.playerReady)
    }

    private fun dispatchViewBegin() {
        if (isReleased || fastPixDataSDK == null) return
        if (enableLogging) {
            Log.d(TAG, "Dispatching ViewBegin event")
        }
        cancelPulseEvent()
        fastPixDataSDK?.dispatchEvent(PlayerEventType.viewBegin)
    }

    private fun dispatchPlay() {
        if (isReleased || fastPixDataSDK == null) return
        if (transitionToEvent(PlayerEvents.PLAY)) {
            if (enableLogging) {
                Log.d(TAG, "Dispatching Play event")
            }
            cancelPulseEvent()
            fastPixDataSDK?.dispatchEvent(PlayerEventType.play)
        }
    }


    override fun playerHeight(): Int? {
        val density = context.resources.displayMetrics.density
        val rawHeight = playerView.measuredHeight
        val height = ceil(rawHeight / density)
        return height.toInt()
    }

    override fun playerWidth(): Int? {
        val density = context.resources.displayMetrics.density
        val rawWidth = playerView.measuredWidth
        val width = ceil(rawWidth / density)
        return width.toInt()
    }

    override fun videoSourceWidth(): Int? {
        return videoSourceWidth
    }

    override fun videoSourceHeight(): Int? {
        return videoSourceHeight
    }

    override fun playHeadTime(): Int? {
        return Utils.secondToMs(currentPosition)
    }

    override fun mimeType(): String? {
        return Utils.getMimeTypeFromUrl(player.source?.config?.url)
    }

    override fun sourceFps(): Int? {
        return null
    }

    override fun sourceAdvertisedBitrate(): String? {
        return null
    }

    override fun sourceAdvertiseFrameRate(): Int? {
        return null
    }

    override fun sourceDuration(): Int? {
        return Utils.secondToMs(player.duration)
    }

    override fun isPause(): Boolean? {
        return player.isPaused
    }

    override fun isAutoPlay(): Boolean? {
        return player.config.playbackConfig.isAutoplayEnabled
    }

    override fun preLoad(): Boolean? {
        return false
    }

    override fun isBuffering(): Boolean? {
        return currentEventState == PlayerEvents.BUFFERING
    }

    override fun playerCodec(): String? {
        return null
    }

    override fun sourceHostName(): String? {
        return null
    }

    override fun isLive(): Boolean? {
        return player.isLive
    }

    override fun sourceUrl(): String? {
        return player.source?.config?.url
    }

    override fun isFullScreen(): Boolean? {
        val orientation = context.resources.configuration.orientation
        return orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    override fun getBandWidthData(): BandwidthModel {
        return bandwidthModel ?: BandwidthModel()
    }

    override fun getPlayerError(): ErrorModel {
        return ErrorModel(errorCode, errorMessage)
    }

    override fun getVideoCodec(): String? {
        return null
    }

    override fun getSoftwareName(): String? {
        return BitMovinLibraryInfo.SDK_NAME
    }

    override fun getSoftwareVersion(): String? {
        return BitMovinLibraryInfo.SDK_VERSION
    }

    fun release() {
        isReleased = true
        cancelPulseEvent()
        if (fastPixDataSDK != null) {
            fastPixDataSDK?.release()
        }
        currentPosition = 0.0
        currentEventState = null
        videoSourceHeight = 0
        videoSourceWidth = 0
        errorCode = null
        lastSeekPosition = 0.0
        errorMessage = null
    }
}