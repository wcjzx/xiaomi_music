package com.example.music_yaoyuhang

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.HomePageInfo
import com.example.music_yaoyuhang.Dao.MusicInfo

class HomePageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val dataList = mutableListOf<HomePageInfo>()

    override fun getItemViewType(position: Int): Int {
        return dataList[position].style
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

            return when (viewType) {
                1 -> BannerViewHolder(inflater.inflate(R.layout.item_banner, parent, false))
                2 -> MusicCardViewHolder(inflater.inflate(R.layout.item_music_card, parent, false))
                3 -> OneColumnViewHolder(inflater.inflate(R.layout.item_one_column, parent, false))
                4 -> TwoColumnViewHolder(inflater.inflate(R.layout.item_two_column, parent, false))
                else -> MusicCardViewHolder(
                    inflater.inflate(
                        R.layout.item_music_card,
                        parent,
                        false
                    )
                )
            }
        }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataList.getOrNull(position)
        if (item != null) {
            when (holder) {
                is BannerViewHolder -> holder.bind(item.musicInfoList)
                is MusicCardViewHolder -> holder.bind(item.musicInfoList)
                is OneColumnViewHolder -> holder.bind(item)
                is TwoColumnViewHolder -> holder.bind(item)
            }
        }
    }
    override fun getItemCount(): Int = dataList.size

    fun setData(newData: List<HomePageInfo>) {
        dataList.clear()
        dataList.addAll(newData)
        notifyDataSetChanged()
    }

    fun addData(newData: List<HomePageInfo>) {
        val startPosition = dataList.size
        dataList.addAll(newData)
        notifyItemRangeInserted(startPosition, newData.size)
    }

    inner class BannerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewPager: ViewPager2 = itemView.findViewById(R.id.bannerViewPager)
        private val indicatorLayout: LinearLayout = itemView.findViewById(R.id.indicatorLayout)

        fun bind(bannerList: List<MusicInfo>) {
                val bannerAdapter = BannerAdapter(bannerList)
            viewPager.adapter = bannerAdapter
        }
    }

    inner class MusicCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewPager: ViewPager2 = itemView.findViewById(R.id.musicCardViewPager)
        private val indicatorLayout: LinearLayout = itemView.findViewById(R.id.indicatorLayout2)

        fun bind(musicList: List<MusicInfo>) {
            val musicCardAdapter = MusicCardAdapter(musicList)
            viewPager.adapter = musicCardAdapter
        }
    }
    inner class OneColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.oneColumnRecyclerView)
        fun bind(homePageInfo: HomePageInfo) {
            val musicInfoList:List<MusicInfo> = homePageInfo.musicInfoList
            recyclerView.layoutManager = LinearLayoutManager(itemView.context)
            recyclerView.adapter = MusicImageAdapter(homePageInfo.musicInfoList,1)
        }
    }

    inner class TwoColumnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recyclerView: RecyclerView = itemView.findViewById(R.id.twoColumnRecyclerView)

        fun bind(homePageInfo: HomePageInfo) {
            recyclerView.layoutManager = GridLayoutManager(itemView.context, 2)
            recyclerView.adapter = MusicImageAdapter(homePageInfo.musicInfoList,2)
        }
    }

    inner class MusicImageAdapter(private val musicInfoList: List<MusicInfo>,num:Int) : RecyclerView.Adapter<MusicImageAdapter.MusicImageViewHolder>() {
var num = num
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicImageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music_image, parent, false)
            return MusicImageViewHolder(view)
        }

        override fun onBindViewHolder(holder: MusicImageViewHolder, position: Int) {
            var widthInPx = 0
            var heightInPx = 0
            if (num == 2) {
                widthInPx = dpToPx(195, holder.itemView.context)
                heightInPx = dpToPx(101, holder.itemView.context)
            }else {
                widthInPx = dpToPx(390, holder.itemView.context)
                heightInPx = dpToPx(144, holder.itemView.context)
            }
            val musicInfo = musicInfoList[position]
            val url = secureUrl(musicInfo.coverUrl)

            val requestOptions = RequestOptions()
                .override(widthInPx, heightInPx) // 指定裁剪后的宽度和高度
                .transform(CenterCrop(), RoundedCorners(30))

            // 加载图片并应用圆角处理
            Glide.with(holder.itemView.context)
                .load(url)
                .apply(requestOptions)
                .into(holder.musicCoverImageView)

            holder.musicNameTextView.text = musicInfo.musicName
            holder.itemView.setOnClickListener {
                val context = holder.itemView.context

                val intent = Intent(context, PlayActivity::class.java).apply {
                    Log.d("TAG1111", "点击了音乐：${musicInfoList}")
                    putExtra("MUSIC_NAME", musicInfo.musicName)
                    putExtra("AUTHOR", musicInfo.author)
                    putExtra("COVER_URL", url)
                    putExtra("MUSIC_URL", musicInfo.musicUrl)
                    putExtra("MUSIC_LIST", ArrayList(musicInfoList))// 添加音乐 URL
                    putExtra("LYRIC_URL", musicInfo.lyricUrl)
                }
                context.startActivity(intent)
            }
            holder.musicCoverImageView.setOnClickListener {
                val context = holder.itemView.context
                val intent = Intent(context, PlayActivity::class.java).apply {
                    Log.d("TAG1111", "点击了音乐：${musicInfoList}")
                    putExtra("MUSIC_NAME", musicInfo.musicName)
                    putExtra("AUTHOR", musicInfo.author)
                    putExtra("COVER_URL", url)
                    putExtra("MUSIC_URL", musicInfo.musicUrl)
                    putExtra("MUSIC_LIST", ArrayList(musicInfoList))// 添加音乐 URL
                    putExtra("LYRIC_URL", musicInfo.lyricUrl)
                }
                context.startActivity(intent)
            }
        }

        // 将 dp 转换为 px 的方法
        fun dpToPx(dp: Int, context: Context): Int {
            return (dp * context.resources.displayMetrics.density).toInt()
        }

        // 确保安全的 URL 方法
        fun secureUrl(url: String?): String? {
            if (url != null && url.startsWith("http://")) {
                return url.replace("http://", "https://")
            }
            return url // 返回原始 URL，如果它已经是 HTTPS 或者是 null
        }

        override fun getItemCount(): Int = musicInfoList.size

        inner class MusicImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val musicCoverImageView: ImageView = itemView.findViewById(R.id.musicCoverImageView)
            val musicNameTextView: TextView = itemView.findViewById(R.id.musicNameTextView)
        }
    }

    fun secureUrl(url: String?): String? {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://")
        }
        return url // 返回原始 URL，如果它已经是 HTTPS 或者是 null
    }
}
