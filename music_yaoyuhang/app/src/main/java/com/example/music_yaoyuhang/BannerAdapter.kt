package com.example.music_yaoyuhang

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.MusicInfo

class BannerAdapter(private val bannerList: List<MusicInfo>) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {
//    private val addButton: Button = itemView.findViewById(R.id.addButton)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_banner_image, parent, false)
        return BannerViewHolder(view)
    }
    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val musicInfo = bannerList[position % bannerList.size]
       val url= secureUrl(musicInfo.coverUrl)
        val requestOptions = RequestOptions()
            .override(390, 101) // 指定裁剪后的宽度和高度
            .transform(CenterCrop(), RoundedCorners(20))
        // 加载图片并应用圆角处理
        Glide.with(holder.itemView.context)
            .load(url)
            .apply(requestOptions)
            .into(holder.bannerImageView)
    }

    override fun getItemCount(): Int = Int.MAX_VALUE // 实现循环滑动

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bannerImageView: ImageView = itemView.findViewById(R.id.bannerImageView)
    }
    fun secureUrl(url: String?): String? {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://")
        }
        return url // 返回原始 URL，如果它已经是 HTTPS 或者是 null
    }
}
