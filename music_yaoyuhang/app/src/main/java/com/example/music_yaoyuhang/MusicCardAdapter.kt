package com.example.music_yaoyuhang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.MusicInfo

class MusicCardAdapter(private val musicList: List<MusicInfo>) : RecyclerView.Adapter<MusicCardAdapter.MusicCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MusicCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicCardViewHolder, position: Int) {
        val musicInfo = musicList[position % musicList.size]
        val url = secureUrl(musicInfo.coverUrl)
        val requestOptions = RequestOptions()
            .override(300, 127) // 指定裁剪后的宽度和高度
            .transform(CenterCrop(), RoundedCorners(20))
        // 加载图片并应用圆角处理
        Glide.with(holder.itemView.context)
            .load(url)
            .apply(requestOptions)
            .into(holder.coverImageView)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    inner class MusicCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImageView: ImageView = itemView.findViewById(R.id.musicImageView)
    }

    fun secureUrl(url: String?): String? {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://")
        }
        return url // 返回原始 URL，如果它已经是 HTTPS 或者是 null
    }
}
