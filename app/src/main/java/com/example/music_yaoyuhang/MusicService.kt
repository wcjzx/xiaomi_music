package com.example.music_yaoyuhang

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer()

        // 创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "music_channel",
                "Music Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val musicUrl = intent.getStringExtra("MUSIC_URL")
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.setDataSource(musicUrl)
            mediaPlayer.prepare()
            mediaPlayer.isLooping = true
            mediaPlayer.start()
        }

        // 创建通知
        val notificationIntent = Intent(this, PlayActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, "music_channel")
            .setContentTitle("Music Playing")
            .setContentText("Your music is playing")
            .setSmallIcon(R.drawable.img)
            .setContentIntent(pendingIntent)
            .build()

        // 启动前台服务
        startForeground(1, notification)

        return START_STICKY
    }

    fun playMusic(musicUrl: String) {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.reset()
        }
        mediaPlayer.setDataSource(musicUrl)
        mediaPlayer.prepare()
        mediaPlayer.start()
    }

    fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    fun resumeMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
        }
    }

    fun isPlaying(): Boolean {
        return mediaPlayer.isPlaying
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
        }
        mediaPlayer.release()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
