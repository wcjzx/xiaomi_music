package com.example.music_yaoyuhang

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.MusicInfo
import com.example.music_yaoyuhang.Request.RetrofitClient
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MainActivity : AppCompatActivity() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val gson = Gson()
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HomePageAdapter
    private lateinit var floatingView: LinearLayout
    private lateinit var currentMusicTextView: TextView
    private lateinit var currentMusicImageView: ImageView
    private lateinit var playPauseButton: ImageButton
    private lateinit var currentMusicAu: TextView
    private lateinit var homelistButton: ImageButton
    private lateinit var sharedPreferences: SharedPreferences
    private var currentPage = 1
    private var isLoading = false
    private var isLastPage = false
    private var musicList1: MutableList<MusicInfo> = mutableListOf() // 添加musicList1变量

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        EventBus.getDefault().register(this)
        initViews()
        setupRecyclerView()
        setupSwipeRefreshLayout()
        setupPlayPauseButton()

        loadData()
    }

    private fun initViews() {
        sharedPreferences = getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        recyclerView = findViewById(R.id.recyclerView)
        floatingView = findViewById(R.id.floatingView)
        currentMusicImageView = findViewById(R.id.currentMusicImageView)
        currentMusicTextView = findViewById(R.id.currentMusicTextView)
        currentMusicAu=findViewById(R.id.currentMusicAu)
        playPauseButton = findViewById(R.id.homeplayPauseButton)
        homelistButton =findViewById(R.id.playlist)
        adapter = HomePageAdapter(this)
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // 检查RecyclerView是否滚动到顶部
                val isAtTop = !recyclerView.canScrollVertically(-1)
                swipeRefreshLayout.isEnabled = isAtTop

                // 检查RecyclerView是否滚动到底部
                if (!recyclerView.canScrollVertically(1) && !isLoading && !isLastPage) {
                    loadMoreData()
                }
            }
        })
    }

    private fun setupSwipeRefreshLayout() {
        swipeRefreshLayout.setOnRefreshListener {
            currentPage = 1
            isLastPage = false
            loadData()
        }
    }

    private fun setupPlayPauseButton() {
        playPauseButton.setOnClickListener {
            // 暂时未实现播放/暂停功能
            Toast.makeText(this, "Play/Pause Button Clicked", Toast.LENGTH_SHORT).show()
        }
        homelistButton.setOnClickListener {
            // 展示播放列表对话框
            showPlaylistDialog()
        }
    }

    private fun loadData() {
        isLoading = true
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.apiService.getHomePageData(currentPage)
                withContext(Dispatchers.Main) {
                    if (response.code == 200) {
                        if (currentPage == 1) {
                            adapter.setData(response.data.records)
                        } else {
                            adapter.addData(response.data.records)
                        }
                        swipeRefreshLayout.isRefreshing = false
                        isLoading = false
                        if (response.data.records.isEmpty()) {
                            isLastPage = true
                        }
                    } else {
                        showToast(response.msg)
                        isLoading = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast(e.message)
                    isLoading = false
                }
            }
        }
    }

    private fun loadMoreData() {
        currentPage++
        loadData()
    }

    override fun onResume() {
        super.onResume()
        updateFloatingView()
    }

    private fun updateFloatingView() {
        CoroutineScope(Dispatchers.IO).launch {
            val musicList = getMusicList()
            withContext(Dispatchers.Main) {
                if (!musicList.isNullOrEmpty()) {
                    val currentMusic = getCurrentPlayingMusic(musicList)
                    if (currentMusic != null) {
                        currentMusicTextView.text = currentMusic.musicName
                    }
                    floatingView.visibility = View.VISIBLE
                } else {
                    floatingView.visibility = View.GONE
                }
            }
        }
    }

    private fun getCurrentPlayingMusic(musicList: List<MusicInfo>): MusicInfo? {
        // 这里可以根据需要修改获取当前播放音乐的逻辑
        return musicList.firstOrNull() // 这里只是示例，返回播放列表的第一首歌
    }

    private fun saveMusicList(musicList: List<MusicInfo>) {
        CoroutineScope(Dispatchers.IO).launch {
            val editor = sharedPreferences.edit()
            val json = gson.toJson(musicList)
            editor.putString("music_list", json)
            editor.apply()
        }
    }

    private fun getMusicList(): List<MusicInfo>? {
        val json = sharedPreferences.getString("music_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<MusicInfo>>() {}.type
            gson.fromJson<List<MusicInfo>>(json, type)
        } else {
            null
        }
    }

    private suspend fun showToast(message: String?) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPlaylistDialog() {
        CoroutineScope(Dispatchers.IO).launch {
            val musicList = getMusicList()
            withContext(Dispatchers.Main) {
                if (!musicList.isNullOrEmpty()) {
                    val dialogView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_playlist, null)
                    val dialog = BottomSheetDialog(this@MainActivity, R.style.BottomSheetDialogTheme)
                    dialog.setContentView(dialogView)

                    val recyclerView: RecyclerView = dialogView.findViewById(R.id.playlistRecyclerView)
                    recyclerView.layoutManager = LinearLayoutManager(this@MainActivity)
                    val adapter = PlaylistAdapter(this@MainActivity, musicList.toMutableList())  // 确保传递的是最新的播放列表
                    recyclerView.adapter = adapter

                    dialog.show()
                } else {
                    showToast("播放列表为空")
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMusicEvent(event: MusicEvent) {
        // 更新 UI
        currentMusicTextView.text = event.musicInfo.musicName
        currentMusicAu.text = event.musicInfo.author
        Glide.with(this)
            .load(event.musicInfo.coverUrl)
            .apply(RequestOptions.bitmapTransform(CircleCrop())) // 圆形图片转换
            .into(currentMusicImageView)


        // 其他 UI 更新操作
    }
    override fun onDestroy() {
        super.onDestroy()
        // 取消注册 EventBus
        EventBus.getDefault().unregister(this)
    }

}
