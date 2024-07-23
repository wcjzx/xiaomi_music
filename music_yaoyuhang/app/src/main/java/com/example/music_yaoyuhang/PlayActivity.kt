package com.example.music_yaoyuhang

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.example.music_yaoyuhang.Dao.MusicInfo
import com.example.music_yaoyuhang.Request.RetrofitClient
import java.io.IOException
import kotlin.random.Random

class PlayActivity : AppCompatActivity() {
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
    lateinit var lyricsTextView: TextView

    enum class PlayMode {
        SEQUENTIAL,
        SHUFFLE,
        REPEAT_ONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_play)

        // 获取从Intent传递的数据
        musicInfo = MusicInfo(
            id = intent.getLongExtra("MUSIC_ID", 1),
            musicName = intent.getStringExtra("MUSIC_NAME").toString(),
            author = intent.getStringExtra("AUTHOR").toString(),
            coverUrl = intent.getStringExtra("COVER_URL").toString(),
            musicUrl = intent.getStringExtra("MUSIC_URL").toString(),
            lyricUrl = intent.getStringExtra("LYRIC_URL").toString()
        )
        Log.d("TAG", "url: ${musicInfo?.lyricUrl.toString()}")
        Log.d("TAG", "musicInfo: $musicInfo")

        val musicListSerializable = intent.getSerializableExtra("MUSIC_LIST")
        val musicList = musicListSerializable as? List<MusicInfo> ?: emptyList()
        MusicManager.playlist.clear()
        MusicManager.playlist.addAll(musicList)
        Log.d("MainActivity", "musicList: $musicList")

        // 初始化视图
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
        lyricsTextView = findViewById(R.id.lyricsTextView)

        playCoverImageView.setOnClickListener {
            playCoverImageView.visibility = View.GONE
            lyricsTextView.visibility = View.VISIBLE
            showLyrics(musicInfo!!.lyricUrl)
        }

        lyricsTextView.setOnClickListener {
            playCoverImageView.visibility = View.VISIBLE
            lyricsTextView.visibility = View.GONE
        }

        nextButton.setOnClickListener {
            playNextSong()
        }
        prevButton.setOnClickListener {
            playPreviousSong()
        }

        // 设置数据到控件
        playMusicNameTextView.text = musicInfo!!.musicName
        playAuthorTextView.text = musicInfo!!.author
        Glide.with(this)
            .load(musicInfo!!.coverUrl)
            .apply(RequestOptions.bitmapTransform(CircleCrop())) // 圆形图片转换
            .into(playCoverImageView)

        // 初始化MediaPlayer
        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnCompletionListener {
            playNextSong()
        }

        handler.post {
            try {
                mediaPlayer.setDataSource(musicInfo!!.musicUrl)
                mediaPlayer.prepare()
                totalTimeTextView.text = formatTime(mediaPlayer.duration)
                seekBar.max = mediaPlayer.duration
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        // 初始化旋转动画
        rotateAnimator = ObjectAnimator.ofFloat(playCoverImageView, "rotation", 0f, 360f).apply {
            duration = 10000 // 10秒旋转一圈
            repeatCount = ObjectAnimator.INFINITE
            repeatMode = ObjectAnimator.RESTART
        }

        // 设置关闭按钮的点击事件
        closeButton.setOnClickListener {
            finish()
        }

        // 设置播放暂停按钮的点击事件
        playPauseButton.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                pauseMusic()
            } else {
                playMusic()
                isPlaying = true
            }
        }

        // 设置循环模式按钮的点击事件
        repeatButton.setOnClickListener {
            playMode = when (playMode) {
                PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
                PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
                PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
            }
            updateRepeatButtonIcon()
        }

        // 设置播放列表按钮的点击事件
        playlistButton.setOnClickListener {
            handler.post {
                showPlaylist()
            }
        }

        // 设置进度条监听
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // 设置播放器的进度监听
        mediaPlayer.setOnBufferingUpdateListener { _, percent ->
            seekBar.secondaryProgress = percent * seekBar.max / 100
        }

        // 更新进度条和时间
        handler.post(updateProgress)

        // 默认播放第一个音乐
        if (MusicManager.playlist.isNotEmpty()) {
            playCurrentSong()
        } else {
            Toast.makeText(this, "播放列表为空", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLyrics(lyricUrl: String) {
        handler.post {
            val lyrics = getLyricsFromServer(lyricUrl)
            val lyricsText = lyrics.joinToString("\n")
            runOnUiThread {
                lyricsTextView.text = lyricsText
            }
        }
    }

    private fun getLyricsFromServer(lyricUrl: String): List<String> {

        return try {
            RetrofitClient.apiService.getLyrics(lyricUrl)
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
        if (MusicManager.playlist.isNotEmpty()) {
            when (playMode) {
                PlayMode.SEQUENTIAL -> {
                    currentSongIndex = (currentSongIndex + 1) % MusicManager.playlist.size
                }
                PlayMode.SHUFFLE -> {
                    currentSongIndex = Random.nextInt(MusicManager.playlist.size)
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

    private fun playPreviousSong() {
        if (MusicManager.playlist.isNotEmpty()) {
            when (playMode) {
                PlayMode.SEQUENTIAL -> {
                    currentSongIndex = if (currentSongIndex - 1 < 0) MusicManager.playlist.size - 1 else currentSongIndex - 1
                }
                PlayMode.SHUFFLE -> {
                    currentSongIndex = Random.nextInt(MusicManager.playlist.size)
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

    private fun playCurrentSong() {
        val currentSong = MusicManager.playlist[currentSongIndex]
        musicInfo = currentSong // 更新当前播放的音乐信息
        handler.post {
            mediaPlayer.reset()
            try {
                mediaPlayer.setDataSource(currentSong.musicUrl)
                mediaPlayer.prepare()
                mediaPlayer.start()
                isPlaying = true
                runOnUiThread {
                    playPauseButton.setImageResource(R.drawable.start)
                    rotateAnimator.start()
                    // 更新UI
                    findViewById<TextView>(R.id.playMusicNameTextView).text = currentSong.musicName
                    findViewById<TextView>(R.id.playAuthorTextView).text = currentSong.author
                    Glide.with(this@PlayActivity)
                        .load(currentSong.coverUrl)
                        .apply(RequestOptions.bitmapTransform(CircleCrop()))
                        .into(playCoverImageView)
                    // 更新总时间和进度条
                    totalTimeTextView.text = formatTime(mediaPlayer.duration)
                    seekBar.max = mediaPlayer.duration
                    seekBar.progress = 0
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

    private fun showPlaylist() {
        val playlistNames = MusicManager.playlist.joinToString("\n") { it.musicName }
        Toast.makeText(this, "播放列表:\n$playlistNames", Toast.LENGTH_SHORT).show()
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
    }

    override fun onPause() {
        super.onPause()
        if (mediaPlayer.isPlaying) {
            serviceIntent = Intent(this, MusicService::class.java).apply {
                putExtra("MUSIC_URL", musicInfo?.musicUrl.toString())
            }
            startService(serviceIntent)
        }
    }
}
