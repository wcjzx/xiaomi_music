package com.example.music_yaoyuhang

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.LyricLine
import com.example.music_yaoyuhang.Dao.MusicInfo
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.greenrobot.eventbus.EventBus
import java.io.IOException
import kotlin.random.Random

class PlayActivity : AppCompatActivity() {
    private lateinit var lyricsRecyclerView: RecyclerView
    private lateinit var lyricsAdapter: LyricsAdapter
    private val lyricsList = mutableListOf<LyricLine>()
    private var serviceIntent: Intent? = null
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlaying = true
    private lateinit var playPauseButton: ImageButton
    private lateinit var playCoverImageView: ImageView
    private lateinit var rotateAnimator: ObjectAnimator
    private lateinit var repeatButton: ImageButton
    private lateinit var playlistButton: ImageButton
    private lateinit var seekBar: SeekBar
    private lateinit var currentTimeTextView: TextView
    private lateinit var totalTimeTextView: TextView
    private var playMode = PlayMode.SEQUENTIAL
    private var currentSongIndex = 0
    private val handler = Handler(Looper.getMainLooper())
    private var musicInfo: MusicInfo? = null
    private var imageConllection: ImageView? = null
    private var isCollected = false
    private val gson = Gson()
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var musicList1: MutableList<MusicInfo>

    enum class PlayMode {
        SEQUENTIAL,
        SHUFFLE,
        REPEAT_ONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        sharedPreferences = getSharedPreferences("music_prefs", Context.MODE_PRIVATE)
        musicList1 = getMusicList()?.toMutableList() ?: mutableListOf()
        Log.d("playActivity", "Music List: $musicList1")
        initViews()
        initMediaPlayer()
        initAnimator()
        initListeners()

        handler.post {
            prepareMediaPlayer()
        }

        handler.post(updateProgress)

        if (musicList1.isNotEmpty()) {
            currentSongIndex = musicList1.indexOfFirst { it.musicName == musicInfo?.musicName }
            playCurrentSong()
        } else {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initViews() {
        lyricsRecyclerView = findViewById(R.id.lyricsRecyclerView)
        lyricsAdapter = LyricsAdapter(lyricsList) {
            lyricsRecyclerView.visibility = View.GONE
            playCoverImageView.visibility = View.VISIBLE
        }
        lyricsRecyclerView.adapter = lyricsAdapter
        lyricsRecyclerView.layoutManager = LinearLayoutManager(this)
        musicInfo = MusicInfo(
            id = intent.getLongExtra("MUSIC_ID", 1),
            musicName = intent.getStringExtra("MUSIC_NAME").toString(),
            author = intent.getStringExtra("AUTHOR").toString(),
            coverUrl = intent.getStringExtra("COVER_URL").toString(),
            musicUrl = intent.getStringExtra("MUSIC_URL").toString(),
            lyricUrl = intent.getStringExtra("LYRIC_URL").toString()
        )

        val musicListSerializable = intent.getSerializableExtra("MUSIC_LIST")
        val musicList = musicListSerializable as? List<MusicInfo> ?: emptyList()
        val updatedMusicList = musicList.toMutableList()
        saveMusicList(updatedMusicList)

        val closeButton: ImageButton = findViewById(R.id.closeButton)
        playCoverImageView = findViewById(R.id.playCoverImageView)
        val playMusicNameTextView: TextView = findViewById(R.id.playMusicNameTextView)
        val playAuthorTextView: TextView = findViewById(R.id.playAuthorTextView)
        seekBar = findViewById(R.id.seekBar)
        currentTimeTextView = findViewById(R.id.currentTimeTextView)
        totalTimeTextView = findViewById(R.id.totalTimeTextView)
        val prevButton: ImageButton = findViewById(R.id.prevButton)
        playPauseButton = findViewById(R.id.playPauseButton)
        val nextButton: ImageButton = findViewById(R.id.nextButton)
        repeatButton = findViewById(R.id.repeatButton)
        playlistButton = findViewById(R.id.playlistButton)
        imageConllection = findViewById(R.id.playconllection)

        Glide.with(this)
            .asBitmap()
            .load(musicInfo!!.coverUrl)
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                    Palette.from(resource).generate { palette ->
                        val dominantColor = palette?.getDominantColor(ContextCompat.getColor(this@PlayActivity, R.color.purple_200))
                        findViewById<LinearLayout>(R.id.mainLayout).setBackgroundColor(dominantColor ?: ContextCompat.getColor(this@PlayActivity, R.color.purple_200))
                    }
                }
                override fun onLoadCleared(placeholder: Drawable?) {}
            })

        playMusicNameTextView.text = musicInfo!!.musicName
        playAuthorTextView.text = musicInfo!!.author

        val url = musicInfo?.coverUrl?.let { secureUrl(it) }
        if (url != null) {
            Glide.with(this)
                .asBitmap()
                .load(url)
                .apply(RequestOptions.bitmapTransform(CircleCrop())) // 圆形图片转换
                .into(playCoverImageView)
        } else {
            playCoverImageView.setImageResource(R.drawable.fill_conllection)
        }
    }

    private fun initMediaPlayer() {
        mediaPlayer = MediaPlayer().apply {
            setOnCompletionListener {
                playNextSong()
            }
        }
    }

    private fun initAnimator() {
        rotateAnimator = ObjectAnimator.ofFloat(playCoverImageView, "rotation", 0f, 360f).apply {
            duration = 10000 // 10秒旋转一圈
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }
    }

    private fun initListeners() {
        findViewById<ImageButton>(R.id.closeButton).setOnClickListener {
            slideDownAndFinish()
        }

        playCoverImageView.setOnClickListener {
            playCoverImageView.visibility = View.GONE
            lyricsRecyclerView.visibility = View.VISIBLE
            showLyrics(musicInfo!!.lyricUrl)
            updatePlaylist(musicInfo!!)
        }

        findViewById<ImageButton>(R.id.nextButton).setOnClickListener {
            playNextSong()
        }

        findViewById<ImageButton>(R.id.prevButton).setOnClickListener {
            playPreviousSong()
        }

        imageConllection?.setOnClickListener {
            toggleCollection()
        }

        playPauseButton.setOnClickListener {
            togglePlayPause()
        }

        repeatButton.setOnClickListener {
            togglePlayMode()
        }

        playlistButton.setOnClickListener {
            handler.post {
                showPlaylistDialog()
            }
        }


        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        mediaPlayer.setOnBufferingUpdateListener { _, percent ->
            seekBar.secondaryProgress = percent * seekBar.max / 100
        }
    }

    private fun prepareMediaPlayer() {
        try {
            mediaPlayer.setDataSource(musicInfo!!.musicUrl)
            mediaPlayer.prepare()
            totalTimeTextView.text = formatTime(mediaPlayer.duration)
            seekBar.max = mediaPlayer.duration
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun showLyrics(lyricUrl: String) {
        Thread {
            val lyrics = getLyricsFromServer(lyricUrl)
            Log.d("showLy", "$lyrics")
            val lyricsList = extractLyricsFromList(lyrics)
            handler.post {
                this.lyricsList.clear()
                this.lyricsList.addAll(lyricsList)
                lyricsAdapter.notifyDataSetChanged()
            }
        }.start()
    }

    private fun getLyricsFromServer(lyricUrl: String): List<String> {
        val client = OkHttpClient()
        val request = Request.Builder().url(lyricUrl).build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")
                val responseBody = response.body()?.string() ?: ""
                val lines = responseBody.lines()
                Log.d("getLyricsFromServer", "response: $lines")
                lines
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private val updateProgress = object : Runnable {
        override fun run() {
            if (mediaPlayer.isPlaying) {
                seekBar.progress = mediaPlayer.currentPosition
                currentTimeTextView.text = formatTime(mediaPlayer.currentPosition)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private fun playNextSong() {
        handler.post {
            if (musicList1.isNotEmpty()) {
                when (playMode) {
                    PlayMode.SEQUENTIAL -> {
                        currentSongIndex = (currentSongIndex + 1) % musicList1.size
                    }
                    PlayMode.SHUFFLE -> {
                        currentSongIndex = Random.nextInt(musicList1.size)
                    }
                    PlayMode.REPEAT_ONE -> {
                        // 保持currentSongIndex不变
                    }
                }
                playCurrentSong()
            } else {
                runOnUiThread {
                    Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playPreviousSong() {
        handler.post {
            if (musicList1.isNotEmpty()) {
                when (playMode) {
                    PlayMode.SEQUENTIAL -> {
                        currentSongIndex = if (currentSongIndex - 1 < 0) musicList1.size - 1 else currentSongIndex - 1
                    }
                    PlayMode.SHUFFLE -> {
                        currentSongIndex = Random.nextInt(musicList1.size)
                    }
                    PlayMode.REPEAT_ONE -> {
                        // 保持currentSongIndex不变
                    }
                }
                playCurrentSong()
            } else {
                runOnUiThread {
                    Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playCurrentSong() {
        Log.d( "playCurrentSong","${musicList1}")
        val currentSong = musicList1[currentSongIndex]
        musicInfo = currentSong // 更新当前播放的音乐信息
        handler.post {
            mediaPlayer.reset()
            try {
                mediaPlayer.setDataSource(currentSong.musicUrl)
                mediaPlayer.prepare()
                mediaPlayer.start()
                isPlaying = true
                runOnUiThread {
                    playPauseButton.setImageResource(R.drawable.play)
                    rotateAnimator.start()
                    // 更新UI
                    findViewById<TextView>(R.id.playMusicNameTextView).text = currentSong.musicName
                    findViewById<TextView>(R.id.playAuthorTextView).text = currentSong.author
                    Glide.with(this@PlayActivity)
                        .load(currentSong.coverUrl)
                        .apply(RequestOptions.bitmapTransform(CircleCrop()))
                        .into(playCoverImageView)

                    // 提取图片的主要颜色并设置背景色
                    Glide.with(this@PlayActivity)
                        .asBitmap()
                        .load(currentSong.coverUrl)
                        .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                            override fun onResourceReady(resource: Bitmap, transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?) {
                                Palette.from(resource).generate { palette ->
                                    val dominantColor = palette?.getDominantColor(ContextCompat.getColor(this@PlayActivity, R.color.purple_200))
                                    findViewById<LinearLayout>(R.id.mainLayout).setBackgroundColor(dominantColor ?: ContextCompat.getColor(this@PlayActivity, R.color.purple_200))
                                }
                            }
                            override fun onLoadCleared(placeholder: Drawable?) {}
                        })

                    // 更新总时间和进度条
                    totalTimeTextView.text = formatTime(mediaPlayer.duration)
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = 0

                    // 更新歌词
                    showLyrics(currentSong.lyricUrl)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun updateRepeatButtonIcon() {
        val iconRes = when (playMode) {
            PlayMode.SEQUENTIAL -> R.drawable.repeat_list
            PlayMode.SHUFFLE -> R.drawable.shuiji
            PlayMode.REPEAT_ONE -> R.drawable.refash
        }
        repeatButton.setImageResource(iconRes)
    }

    private fun togglePlayPause() {
        handler.post {
            if (mediaPlayer.isPlaying) {
                pauseMusic()
            } else {
                playMusic()
                isPlaying = true
            }
        }
    }

    private fun playMusic() {
        handler.post {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
                isPlaying = true
                // 更新播放暂停按钮图标
                playPauseButton.setImageResource(R.drawable.play)
                // 开始旋转动画
                rotateAnimator.start()
            }
        }
    }

    private fun pauseMusic() {
        handler.post {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
                // 更新播放暂停按钮图标
                playPauseButton.setImageResource(R.drawable.start)
                // 暂停旋转动画
                rotateAnimator.pause()
            }
        }
    }

    private fun formatTime(timeInMillis: Int): String {
        val minutes = timeInMillis / 1000 / 60
        val seconds = timeInMillis / 1000 % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun showPlaylistDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_playlist, null)
        val dialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        dialog.setContentView(dialogView)

        val recyclerView: RecyclerView = dialogView.findViewById(R.id.playlistRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PlaylistAdapter(this, musicList1)
        Log.d("showPlaylistDialog","${musicList1}")// 确保传递的是最新的播放列表
        recyclerView.adapter = adapter

        dialog.show()
    }

    fun extractLyricsFromList(lyricsList: List<String>): List<LyricLine> {
        val regex = "\\[(\\d{2}):(\\d{2})\\.(\\d{2})]".toRegex()  // 匹配时间标签的正则表达式
        val extractedLyrics = mutableListOf<LyricLine>()

        for (line in lyricsList) {
            val matchResult = regex.find(line)
            if (matchResult != null) {
                val (minutes, seconds, centiseconds) = matchResult.destructured
                val timeInMillis = (minutes.toLong() * 60 * 1000) + (seconds.toLong() * 1000) + (centiseconds.toLong() * 10)
                val text = line.replace(regex, "").trim()  // 去掉时间标签并清理多余的空白
                if (text.isNotEmpty()) {
                    extractedLyrics.add(LyricLine(timeInMillis, text))
                }
            }
        }
        return extractedLyrics
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun playCollectAnimation() {
        val scaleX = ObjectAnimator.ofFloat(imageConllection, "scaleX", 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(imageConllection, "scaleY", 1f, 1.2f, 1f)
        val rotateY = ObjectAnimator.ofFloat(imageConllection, "rotationY", 0f, 360f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, rotateY)
        animatorSet.duration = 1000
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    @SuppressLint("ObjectAnimatorBinding")
    private fun playUncollectAnimation() {
        val scaleX = ObjectAnimator.ofFloat(imageConllection, "scaleX", 1f, 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(imageConllection, "scaleY", 1f, 0.8f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 1000
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun getMusicList(): List<MusicInfo>? {
        val json = sharedPreferences.getString("music_list", null)
        return if (json != null) {
            val type = object : TypeToken<List<MusicInfo>>() {}.type
            gson.fromJson(json, type)
        } else {
            null
        }
    }

    private fun saveMusicList(newMusicInfos: List<MusicInfo>) {
        // 获取当前存储的音乐列表
        val currentMusicList = getMusicList()?.toMutableList() ?: mutableListOf()

        // 遍历新的音乐信息列表
        newMusicInfos.forEach { newMusicInfo ->
            // 检查要添加的音乐是否已经存在于列表中
            val exists = currentMusicList.any { it.id == newMusicInfo.id }

            // 如果不存在，则添加到列表中
            if (!exists) {
                currentMusicList.add(newMusicInfo)
            }
        }
        // 将更新后的列表存储到 SharedPreferences 中
        val editor = sharedPreferences.edit()
        val json = gson.toJson(currentMusicList)
        editor.putString("music_list", json)
        editor.apply()
    }

    private fun updatePlaylist(musicInfo: MusicInfo) {
        val updatedMusicList = musicList1.toMutableList()
        updatedMusicList.add(musicInfo)
        saveMusicList(updatedMusicList)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
        // 停止服务
        stopService(Intent(this, MusicService::class.java))
        // 释放动画资源
        rotateAnimator.cancel()
        handler.removeCallbacks(updateProgress)

        // 发布当前正在播放的歌曲信息
        EventBus.getDefault().post(MusicEvent(musicInfo!!))
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            pauseMusic()
        }
    }

    private fun toggleCollection() {
        isCollected = !isCollected
        val updatedMusicList = musicList1.toMutableList()
        if (isCollected) {
            imageConllection?.setImageResource(R.drawable.fill_conllection)
            playCollectAnimation()
            updatedMusicList.add(musicInfo!!)
            saveMusicList(updatedMusicList)
        } else {
            imageConllection?.setImageResource(R.drawable.collection)
            playUncollectAnimation()
            // Remove from the collection if needed
            updatedMusicList.removeAll { it.id == musicInfo!!.id }
            saveMusicList(updatedMusicList)
        }
    }

    private fun togglePlayMode() {
        playMode = when (playMode) {

            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
        }



        updateRepeatButtonIcon()
        var array: ArrayList<String>

    }

    private fun slideDownAndFinish() {
        val slideDown = ObjectAnimator.ofFloat(window.decorView, "translationY", 0f, window.decorView.height.toFloat())
        slideDown.duration = 500
        slideDown.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                finish()
                overridePendingTransition(0, 0) // 禁用默认的动画效果
            }
        })
        slideDown.start()
    }

    fun secureUrl(url: String?): String? {
        if (url != null && url.startsWith("http://")) {
            return url.replace("http://", "https://")
        }
        return url // 返回原始 URL，如果它已经是 HTTPS 或者是 null
    }
}
