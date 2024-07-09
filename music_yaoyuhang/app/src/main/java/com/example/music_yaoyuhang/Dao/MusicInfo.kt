package com.example.music_yaoyuhang.Dao

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class MusicInfo(
    var id: Long,
    var musicName: String,
    var author: String,
    var coverUrl: String,
    var musicUrl: String,
    var lyricUrl: String
): Serializable



data class HomePageInfo(
    val moduleConfigId: Int,
    val moduleName: String,
    val style: Int,
    val musicInfoList: List<MusicInfo>
)

data class HomePageResponse(
    val code: Int,
    val msg: String,
    val data: Page<HomePageInfo>
)

data class Page<T>(
    val records: List<T>,
    val total: Int,
    val size: Int,
    val current: Int,
    val pages: Int
)
