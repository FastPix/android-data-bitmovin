package io.fastpix.bitmovin_player_data.src.utils

import kotlin.math.floor

object Utils {
    fun getMimeTypeFromUrl(url: String?): String? {
        return when {
            url?.endsWith(".m3u8", true) == true -> "application/x-mpegURL"
            url?.endsWith(".mpd", true) == true -> "application/dash+xml"
            url?.endsWith(".mp4", true) == true -> "video/mp4"
            else -> null
        }
    }

    fun secondToMs(seconds: Double): Int = floor(seconds * 1000).toInt()

    val validTransitions = mapOf(
        null to setOf(PlayerEvents.PLAY, PlayerEvents.ERROR),
        PlayerEvents.PLAY to setOf(
            PlayerEvents.PLAYING,
            PlayerEvents.ENDED,
            PlayerEvents.PAUSE,
            PlayerEvents.VARIANT_CHANGED,
            PlayerEvents.SEEKING,
            PlayerEvents.ERROR
        ),
        PlayerEvents.PLAYING to setOf(
            PlayerEvents.BUFFERING,
            PlayerEvents.PAUSE,
            PlayerEvents.ENDED,
            PlayerEvents.SEEKING,
            PlayerEvents.VARIANT_CHANGED,
            PlayerEvents.ERROR
        ),
        PlayerEvents.BUFFERING to setOf(
            PlayerEvents.BUFFERED,
            PlayerEvents.ERROR,
            PlayerEvents.VARIANT_CHANGED
        ),
        PlayerEvents.BUFFERED to setOf(
            PlayerEvents.PAUSE,
            PlayerEvents.SEEKING,
            PlayerEvents.PLAYING,
            PlayerEvents.ENDED,
            PlayerEvents.ERROR,
            PlayerEvents.VARIANT_CHANGED
        ),
        PlayerEvents.PAUSE to setOf(
            PlayerEvents.SEEKING,
            PlayerEvents.PLAY,
            PlayerEvents.ENDED,
            PlayerEvents.ERROR,
            PlayerEvents.VARIANT_CHANGED
        ),
        PlayerEvents.SEEKING to setOf(
            PlayerEvents.SEEKED,
            PlayerEvents.ENDED,
            PlayerEvents.ERROR,
            PlayerEvents.VARIANT_CHANGED
        ),
        PlayerEvents.SEEKED to setOf(
            PlayerEvents.PLAY, PlayerEvents.ENDED, PlayerEvents.ERROR, PlayerEvents.VARIANT_CHANGED,
            PlayerEvents.PLAYING, PlayerEvents.SEEKING
        ),
        PlayerEvents.ENDED to setOf(
            PlayerEvents.PLAY,
            PlayerEvents.PAUSE,
            PlayerEvents.ERROR,
            PlayerEvents.VARIANT_CHANGED,
        ),
        PlayerEvents.ERROR to setOf(
            PlayerEvents.PLAYING, PlayerEvents.PLAY, PlayerEvents.PAUSE,
            PlayerEvents.BUFFERED
        )
    )
}

