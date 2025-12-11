package io.fastpix.bitmovin_player_data.src

import io.fastpix.data.domain.model.CustomDataDetails
import io.fastpix.data.domain.model.PlayerDataDetails
import io.fastpix.data.domain.model.VideoDataDetails

data class CustomerData(
    var beaconUrl: String? = null,
    var workspaceId: String,
    var videoDetails: VideoDataDetails? = null,
    var playerDetails: PlayerDataDetails = PlayerDataDetails("bitmovin-player", "3.+"),
    var customDataDetails: CustomDataDetails? = null
)