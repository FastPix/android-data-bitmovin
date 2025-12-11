package io.fastpix.bitmovin_data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


val dummyData = listOf<DummyData>(
    DummyData(
        "Tears of Steel",
        "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
    ),
    DummyData(
        "Big Buck Bunny",
        "https://cdn.theoplayer.com/video/big_buck_bunny/big_buck_bunny_metadata.m3u8",
    ),
    DummyData(
        "Star Ward Episode",
        "https://cdn.theoplayer.com/video/star_wars_episode_vii-the_force_awakens_official_comic-con_2015_reel_(2015)/index.m3u8",
    ),
)


@Parcelize
data class DummyData(
    var id: String,
    var url: String,
) : Parcelable